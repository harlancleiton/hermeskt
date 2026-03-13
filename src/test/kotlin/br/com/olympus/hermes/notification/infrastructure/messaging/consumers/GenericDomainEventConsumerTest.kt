package br.com.olympus.hermes.notification.infrastructure.messaging.consumers

import arrow.core.left
import arrow.core.right
import br.com.olympus.hermes.notification.application.ports.IncomingDomainEvent
import br.com.olympus.hermes.notification.application.ports.UpstreamDomainEventHandler
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [GenericDomainEventConsumer] covering the three equivalence partitions from the
 * test strategy: valid payloads, malformed JSON, and handler failures.
 *
 * Test design techniques applied: Equivalence Partitioning, Boundary Value Analysis.
 */
class GenericDomainEventConsumerTest {
    private val handler: UpstreamDomainEventHandler = mockk()
    private val objectMapper: ObjectMapper = ObjectMapper()
    private val consumer = GenericDomainEventConsumer(handler, objectMapper)

    // ---- Equivalence Partition: valid JSON with eventType and payload ----

    @Test
    fun `consume valid JSON delegates correct IncomingDomainEvent to handler`() {
        val captured = slot<IncomingDomainEvent>()
        every { handler.handle(capture(captured)) } returns Unit.right()

        val json =
            """{"eventType":"User2FACodeRequested","occurredAt":1700000000000,"payload":{"userId":"abc","code":"123456"}}"""

        consumer.consume(json)

        verify(exactly = 1) { handler.handle(any()) }
        assertEquals("User2FACodeRequested", captured.captured.eventType)
        assertEquals("abc", captured.captured.payload["userId"])
        assertEquals("123456", captured.captured.payload["code"])
    }

    @Test
    fun `consume valid JSON without payload field defaults payload to empty map`() {
        val captured = slot<IncomingDomainEvent>()
        every { handler.handle(capture(captured)) } returns Unit.right()

        val json = """{"eventType":"SomeEvent","occurredAt":1700000000000}"""

        consumer.consume(json)

        verify(exactly = 1) { handler.handle(any()) }
        assertTrue(captured.captured.payload.isEmpty())
    }

    // ---- Boundary Value: occurredAt absent → fallback to current time ----

    @Test
    fun `consume JSON without occurredAt field uses current time as fallback`() {
        val captured = slot<IncomingDomainEvent>()
        every { handler.handle(capture(captured)) } returns Unit.right()

        val before = System.currentTimeMillis()
        consumer.consume("""{"eventType":"SomeEvent","payload":{"key":"value"}}""")

        val capturedTime = captured.captured.occurredAt.time
        assertTrue(capturedTime >= before - 1000, "occurredAt should be approximately now")
    }

    // ---- Equivalence Partition: malformed JSON ----

    @Test
    fun `consume malformed JSON skips message without invoking handler`() {
        consumer.consume("not-valid-json{{{{")

        verify(exactly = 0) { handler.handle(any()) }
    }

    // ---- Equivalence Partition: valid JSON but missing eventType ----

    @Test
    fun `consume JSON without eventType field skips message without invoking handler`() {
        consumer.consume("""{"occurredAt":1700000000000,"payload":{"foo":"bar"}}""")

        verify(exactly = 0) { handler.handle(any()) }
    }

    // ---- Equivalence Partition: handler returns Left (business error → DLQ routing) ----

    @Test
    fun `consume when handler returns Left throws RuntimeException to trigger nack and DLQ routing`() {
        val error = PersistenceError(reason = "template not found")
        every { handler.handle(any()) } returns error.left()

        val json = """{"eventType":"User2FACodeRequested","occurredAt":1700000000000,"payload":{}}"""

        assertThrows(RuntimeException::class.java) { consumer.consume(json) }
    }

    @Test
    fun `consume when handler returns Right completes without exception`() {
        every { handler.handle(any()) } returns Unit.right()

        val json = """{"eventType":"PasswordResetRequested","occurredAt":1700000000000,"payload":{"email":"user@example.com"}}"""

        consumer.consume(json)

        verify(exactly = 1) { handler.handle(any()) }
    }
}
