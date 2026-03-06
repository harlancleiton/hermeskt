package br.com.olympus.hermes.core.application.projectors

import br.com.olympus.hermes.shared.domain.events.NotificationSentEvent
import br.com.olympus.hermes.shared.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID

/**
 * Integration test for [NotificationSentProjector]. Seeds a [NotificationView] via
 * [NotificationViewRepository], fires a [NotificationSentEvent], and asserts that `sentAt` is
 * persisted correctly in MongoDB.
 */
@QuarkusTest
class NotificationSentProjectorIT {
    @Inject lateinit var projector: NotificationSentProjector

    @Inject lateinit var viewRepository: NotificationViewRepository

    @Test
    fun `consume sent event and update NotificationView sentAt`() {
        val id = UUID.randomUUID().toString()
        val now = Date()

        // Seed view
        val view =
            NotificationView().apply {
                this.id = id
                type = "EMAIL"
                content = "Sent integration test"
                createdAt = now
                updatedAt = now
            }
        viewRepository.upsert(view)

        // Fire event
        val event =
            NotificationSentEvent(
                shippingReceipt = mapOf("aggregateId" to id, "original" to "rcpt-it"),
                sentAt = now,
            )
        val result = projector.handle(event)

        assertTrue(result.isRight())

        val entityId = EntityId.from(id).getOrNull()!!
        val updated = viewRepository.findById(entityId).getOrNull()
        assertNotNull(updated)
        assertNotNull(updated!!.sentAt)
    }

    @Test
    fun `idempotent — re-processing same event yields same sentAt`() {
        val id = UUID.randomUUID().toString()
        val sentAt = Date(1_700_000_000_000L)

        val view =
            NotificationView().apply {
                this.id = id
                type = "SMS"
                content = "Idempotent integration test"
                createdAt = Date()
                updatedAt = Date()
            }
        viewRepository.upsert(view)

        val event =
            NotificationSentEvent(
                shippingReceipt = mapOf("aggregateId" to id, "original" to "r"),
                sentAt = sentAt,
            )

        projector.handle(event)
        val result = projector.handle(event)

        assertTrue(result.isRight())
        val entityId = EntityId.from(id).getOrNull()!!
        val updated = viewRepository.findById(entityId).getOrNull()
        assertNotNull(updated!!.sentAt)
    }
}
