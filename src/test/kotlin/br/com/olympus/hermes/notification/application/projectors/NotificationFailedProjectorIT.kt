package br.com.olympus.hermes.notification.application.projectors

import br.com.olympus.hermes.notification.domain.events.*
import br.com.olympus.hermes.notification.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.notification.infrastructure.readmodel.NotificationView
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID

/**
 * Integration test for [NotificationFailedProjector]. Seeds a [NotificationView] via
 * [NotificationViewRepository], fires a [NotificationDeliveryFailedEvent], and asserts that
 * `failureReason` is persisted correctly in MongoDB.
 */
@QuarkusTest
class NotificationFailedProjectorIT {
    @Inject lateinit var projector: NotificationFailedProjector

    @Inject lateinit var viewRepository: NotificationViewRepository

    @Test
    fun `consume failed event and update NotificationView failureReason`() {
        val id = UUID.randomUUID().toString()
        val now = Date()

        // Seed view
        val view =
            NotificationView().apply {
                this.id = id
                type = "PUSH"
                content = "Failed integration test"
                createdAt = now
                updatedAt = now
            }
        viewRepository.upsert(view)

        val event =
            NotificationDeliveryFailedEvent(
                aggregateId = id,
                reason = "Provider returned HTTP 503",
                failedAt = now,
            )
        val result = projector.handle(event)

        assertTrue(result.isRight())

        val entityId = EntityId.from(id).getOrNull()!!
        val updated = viewRepository.findById(entityId).getOrNull()
        assertNotNull(updated)
        assertEquals("Provider returned HTTP 503", updated!!.failureReason)
    }

    @Test
    fun `idempotent — re-processing same event yields same failureReason`() {
        val id = UUID.randomUUID().toString()
        val reason = "Network timeout"

        val view =
            NotificationView().apply {
                this.id = id
                type = "EMAIL"
                content = "Idempotent failed integration test"
                createdAt = Date()
                updatedAt = Date()
            }
        viewRepository.upsert(view)

        val event =
            NotificationDeliveryFailedEvent(
                aggregateId = id,
                reason = reason,
                failedAt = Date(),
            )

        projector.handle(event)
        val result = projector.handle(event)

        assertTrue(result.isRight())
        val entityId = EntityId.from(id).getOrNull()!!
        val updated = viewRepository.findById(entityId).getOrNull()
        assertEquals(reason, updated!!.failureReason)
    }
}
