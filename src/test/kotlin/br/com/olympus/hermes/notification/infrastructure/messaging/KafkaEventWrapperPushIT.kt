package br.com.olympus.hermes.notification.infrastructure.messaging

import br.com.olympus.hermes.notification.domain.events.*
import br.com.olympus.hermes.notification.domain.valueobjects.DeviceToken
import br.com.olympus.hermes.shared.domain.events.EventWrapper
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper.Companion.toNotificationCreatedEvent
import java.util.Date
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KafkaEventWrapperPushIT {
    @Test
    fun `round-trip serialization PushNotificationCreatedEvent to KafkaEventWrapper to PushNotificationCreatedEvent`() {
        val id = UUID.randomUUID().toString()
        val originalEvent =
                PushNotificationCreatedEvent(
                        aggregateId = id,
                        content = "Hello Push",
                        payload = mapOf("key" to "value"),
                        deviceToken = DeviceToken.create("token123").getOrNull()!!,
                        title = "Push Title",
                        data = mapOf("custom" to "dataKey"),
                )

        val envelope =
                EventWrapper(
                        aggregateId = EntityId.from(id).getOrNull()!!,
                        aggregateType = "PushNotification",
                        aggregateVersion = 1,
                        eventType = "PushNotificationCreatedEvent",
                        occurredAt = Date(),
                        payload = originalEvent,
                )

        val kafkaWrapper = KafkaEventWrapper.from(envelope)
        assertEquals("PushNotificationCreatedEvent", kafkaWrapper.eventType)
        assertEquals("token123", kafkaWrapper.payload["deviceToken"])

        val reconstructedEvent = kafkaWrapper.toNotificationCreatedEvent()
        assertTrue(reconstructedEvent is PushNotificationCreatedEvent)
        reconstructedEvent as PushNotificationCreatedEvent

        assertEquals(originalEvent.aggregateId, reconstructedEvent.aggregateId)
        assertEquals(originalEvent.content, reconstructedEvent.content)
        assertEquals(originalEvent.payload, reconstructedEvent.payload)
        assertEquals(originalEvent.deviceToken.value, reconstructedEvent.deviceToken.value)
        assertEquals(originalEvent.title, reconstructedEvent.title)
        assertEquals(originalEvent.data, reconstructedEvent.data)
    }

    @Test
    fun `toNotificationCreatedEvent returns null for unknown event types`() {
        val kafkaWrapper =
                KafkaEventWrapper(
                        eventType = "UnknownEvent",
                        occurredAt = Date(),
                        payload = emptyMap<String, Any?>(),
                )

        assertNull(kafkaWrapper.toNotificationCreatedEvent())
    }
}
