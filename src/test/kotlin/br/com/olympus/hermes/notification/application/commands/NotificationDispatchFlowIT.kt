package br.com.olympus.hermes.notification.application.commands

import br.com.olympus.hermes.notification.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * End-to-end integration test for the notification dispatch flow. Fires a
 * [CreateNotificationCommand] through the [CreateNotificationHandler] and asserts that:
 * 1. The command returns [arrow.core.Either.Right].
 * 2. A [br.com.olympus.hermes.notification.infrastructure.readmodel.NotificationView] is
 *    eventually projected into MongoDB by the Kafka consumer chain.
 *
 * Relies on Quarkus Dev Services to spin up Kafka, MongoDB, and DynamoDB (LocalStack)
 * automatically.
 */
@QuarkusTest
class NotificationDispatchFlowIT {
    @Inject
    lateinit var handler: CreateNotificationHandler

    @Inject
    lateinit var viewRepository: NotificationViewRepository

    @Test
    fun `push notification command persists to DynamoDB and projects view to MongoDB`() {
        val id = UUID.randomUUID().toString()
        val command =
            CreateNotificationCommand.Push(
                id = id,
                deviceToken = "e2e-device-token",
                title = "E2E Alert",
                content = "Integration test body",
                payload = mapOf("test" to "e2e"),
                data = mapOf("action" to "open"),
            )

        val result = handler.handle(command)

        assertTrue(result.isRight(), "Handler must return Right but got: ${result.leftOrNull()}")

        val entityId = EntityId.from(id).getOrNull()!!
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until {
                viewRepository.findById(entityId).getOrNull() != null
            }

        val view = viewRepository.findById(entityId).getOrNull()
        assertNotNull(view, "NotificationView must exist in MongoDB after projection")
        assertEquals(id, view!!.id)
        assertEquals("PUSH", view.type)
        assertEquals("e2e-device-token", view.deviceToken)
        assertEquals("E2E Alert", view.title)
        assertEquals("PENDING", view.status)
        assertEquals("Integration test body", view.content)
    }

    @Test
    fun `sms notification command persists to DynamoDB and projects view to MongoDB`() {
        val id = UUID.randomUUID().toString()
        val command =
            CreateNotificationCommand.Sms(
                id = id,
                from = 12345u,
                to = "11999887766",
                content = "Your OTP is 999999",
                payload = emptyMap(),
            )

        val result = handler.handle(command)

        assertTrue(result.isRight(), "Handler must return Right but got: ${result.leftOrNull()}")

        val entityId = EntityId.from(id).getOrNull()!!
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until {
                viewRepository.findById(entityId).getOrNull() != null
            }

        val view = viewRepository.findById(entityId).getOrNull()
        assertNotNull(view, "NotificationView must exist in MongoDB after projection")
        assertEquals(id, view!!.id)
        assertEquals("SMS", view.type)
        assertEquals("11999887766", view.to)
        assertEquals("PENDING", view.status)
    }
}
