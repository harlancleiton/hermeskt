package br.com.olympus.hermes.core.application.projectors

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.events.NotificationDeliveryFailedEvent
import br.com.olympus.hermes.shared.domain.exceptions.NotificationNotFoundError
import br.com.olympus.hermes.shared.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date

class NotificationFailedProjectorTest {
    private val viewRepository: NotificationViewRepository = mockk()
    private val projector = NotificationFailedProjector(viewRepository)

    private fun makeEvent(aggregateId: String): NotificationDeliveryFailedEvent =
        NotificationDeliveryFailedEvent(
            aggregateId = aggregateId,
            reason = "Provider timeout",
            failedAt = Date(),
        )

    @Test
    fun `should update NotificationView failureReason on valid event`() {
        val aggregateId = "550e8400-e29b-41d4-a716-446655440010"
        val view = NotificationView().apply { id = aggregateId }

        every { viewRepository.findById(any()) } returns Either.Right(view)
        every { viewRepository.upsert(any()) } returns Either.Right(Unit)

        val result = projector.handle(makeEvent(aggregateId))

        assertTrue(result.isRight())
        verify { viewRepository.upsert(match { it.failureReason == "Provider timeout" }) }
    }

    @Test
    fun `should return NotificationNotFoundError when view does not exist`() {
        val aggregateId = "550e8400-e29b-41d4-a716-446655440011"

        every { viewRepository.findById(any()) } returns Either.Right(null)

        val result = projector.handle(makeEvent(aggregateId))

        assertTrue(result.isLeft())
        result.onLeft { assertInstanceOf(NotificationNotFoundError::class.java, it) }
    }

    @Test
    fun `should be idempotent — re-processing same event yields same state`() {
        val aggregateId = "550e8400-e29b-41d4-a716-446655440012"
        val reason = "HTTP 503 from provider"
        val view =
            NotificationView().apply {
                id = aggregateId
                failureReason = reason
            }
        val event =
            NotificationDeliveryFailedEvent(
                aggregateId = aggregateId,
                reason = reason,
                failedAt = Date(),
            )

        every { viewRepository.findById(any()) } returns Either.Right(view)
        every { viewRepository.upsert(any()) } returns Either.Right(Unit)

        projector.handle(event)
        val result = projector.handle(event)

        assertTrue(result.isRight())
        verify(exactly = 2) { viewRepository.upsert(match { it.failureReason == reason }) }
    }
}
