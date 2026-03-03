package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DomainEventSerdeWhatsAppTest {

    private lateinit var serde: DomainEventSerde

    private val from = BrazilianPhone.create("11987654321").getOrNull()!!
    private val to = BrazilianPhone.create("11912345678").getOrNull()!!

    @BeforeEach
    fun setUp() {
        serde = DomainEventSerde(ObjectMapper())
    }

    @Test
    fun `serialize_deserialize_roundTrip`() {
        val eventId = EntityId.generate()
        val aggregateId = EntityId.generate()
        val now = Date()
        val original =
                WhatsAppNotificationCreatedEvent(
                        id = eventId,
                        aggregateId = aggregateId,
                        aggregateVersion = 0,
                        occurredAt = now,
                        content = "Hello, world!",
                        payload = mapOf("key" to "value"),
                        from = from,
                        to = to,
                        templateName = "hello_world"
                )

        val json = serde.serialize(original)
        val result =
                serde.deserialize(
                        eventType = "WhatsAppNotificationCreatedEvent",
                        eventId = eventId,
                        aggregateId = aggregateId,
                        version = 0,
                        occurredAt = now,
                        json = json
                )

        assertTrue(result.isRight())
        val deserialized = (result as Either.Right).value
        assertTrue(deserialized is WhatsAppNotificationCreatedEvent)
        val event = deserialized as WhatsAppNotificationCreatedEvent
        assertEquals(original.content, event.content)
        assertEquals(original.from, event.from)
        assertEquals(original.to, event.to)
        assertEquals(original.templateName, event.templateName)
    }

    @Test
    fun `deserialize_withInvalidPhone_returnsLeft`() {
        val json =
                """{"content":"Hello","payload":{},"from":"invalid-phone","to":"11912345678","templateName":"hello_world"}"""

        val result =
                serde.deserialize(
                        eventType = "WhatsAppNotificationCreatedEvent",
                        eventId = EntityId.generate(),
                        aggregateId = EntityId.generate(),
                        version = 0,
                        occurredAt = Date(),
                        json = json
                )

        assertTrue(result.isLeft())
    }
}
