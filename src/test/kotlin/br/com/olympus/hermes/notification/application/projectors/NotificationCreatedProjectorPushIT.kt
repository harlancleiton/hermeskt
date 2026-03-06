package br.com.olympus.hermes.notification.application.projectors

import br.com.olympus.hermes.notification.domain.events.*
import br.com.olympus.hermes.notification.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.notification.domain.valueobjects.DeviceToken
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class NotificationCreatedProjectorPushIT {
    @Inject
    lateinit var projector: NotificationCreatedProjector

    @Inject
    lateinit var viewRepository: NotificationViewRepository

    @Test
    fun `consume should project PushNotificationCreatedEvent into NotificationView with type PUSH`() {
        val id = UUID.randomUUID().toString()
        val event =
            PushNotificationCreatedEvent(
                aggregateId = id,
                content = "Push Content",
                payload = emptyMap(),
                deviceToken = DeviceToken.create("valid-token").getOrNull()!!,
                title = "Push Title",
                data = emptyMap(),
            )

        val result = projector.handle(event)
        assertTrue(result.isRight())

        val entityId = EntityId.from(id).getOrNull()!!
        val savedView = viewRepository.findById(entityId).getOrNull()
        assertNotNull(savedView)
        assertEquals("PUSH", savedView!!.type)
        assertEquals("valid-token", savedView.deviceToken)
        assertEquals("Push Title", savedView.title)
    }

    @Test
    fun `consume should be idempotent`() {
        val id = UUID.randomUUID().toString()
        val event =
            PushNotificationCreatedEvent(
                aggregateId = id,
                content = "Push Content",
                payload = emptyMap(),
                deviceToken = DeviceToken.create("valid-token").getOrNull()!!,
                title = "Push Title",
                data = emptyMap(),
            )

        // Process once
        projector.handle(event)

        // Process again
        val result = projector.handle(event)
        assertTrue(result.isRight())

        val entityId = EntityId.from(id).getOrNull()!!
        val savedView = viewRepository.findById(entityId).getOrNull()
        assertNotNull(savedView)
    }
}
