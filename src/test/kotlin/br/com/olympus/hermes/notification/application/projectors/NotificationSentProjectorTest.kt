package br.com.olympus.hermes.notification.application.projectors

import arrow.core.Either
import br.com.olympus.hermes.notification.domain.events.*
import br.com.olympus.hermes.notification.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.notification.infrastructure.readmodel.NotificationView
import br.com.olympus.hermes.shared.domain.exceptions.NotificationNotFoundError
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date

class NotificationSentProjectorTest {
    private val viewRepository: NotificationViewRepository = mockk()
    private val projector = NotificationSentProjector(viewRepository)

    private fun makeEvent(aggregateId: String): NotificationSentEvent =
        NotificationSentEvent(
            shippingReceipt = mapOf("aggregateId" to aggregateId, "original" to "rcpt-123"),
            sentAt = Date(),
        )

    @Test
    fun `should update NotificationView sentAt on valid event`() {
        val aggregateId = "550e8400-e29b-41d4-a716-446655440000"
        val view = NotificationView().apply { id = aggregateId }

        every { viewRepository.findById(any()) } returns Either.Right(view)
        every { viewRepository.upsert(any()) } returns Either.Right(Unit)

        val result = projector.handle(makeEvent(aggregateId))

        assertTrue(result.isRight())
        verify { viewRepository.upsert(match { it.sentAt != null }) }
    }

    @Test
    fun `should return NotificationNotFoundError when view does not exist`() {
        val aggregateId = "550e8400-e29b-41d4-a716-446655440001"

        every { viewRepository.findById(any()) } returns Either.Right(null)

        val result = projector.handle(makeEvent(aggregateId))

        assertTrue(result.isLeft())
        result.onLeft { assertInstanceOf(NotificationNotFoundError::class.java, it) }
    }

    @Test
    fun `should be idempotent — re-processing same event yields same state`() {
        val aggregateId = "550e8400-e29b-41d4-a716-446655440002"
        val sentAt = Date(1_000_000L)
        val view =
            NotificationView().apply {
                id = aggregateId
                this.sentAt = sentAt
            }
        val event =
            NotificationSentEvent(
                shippingReceipt = mapOf("aggregateId" to aggregateId, "original" to "r"),
                sentAt = sentAt,
            )

        every { viewRepository.findById(any()) } returns Either.Right(view)
        every { viewRepository.upsert(any()) } returns Either.Right(Unit)

        // First
        projector.handle(event)
        // Second (idempotent replay)
        val result = projector.handle(event)

        assertTrue(result.isRight())
        verify(exactly = 2) { viewRepository.upsert(match { it.sentAt == sentAt }) }
    }
}
