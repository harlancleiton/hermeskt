package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.events.PushNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.valueobjects.DeviceToken
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date

class PushNotificationTest {
    @Test
    fun `should raise PushNotificationCreatedEvent when isNew is true`() {
        val id = EntityId.generate()
        val deviceToken = DeviceToken.create("token123").getOrNull()!!

        val notification =
            PushNotification(
                content = "body",
                payload = mapOf("key" to "value"),
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                id = id,
                createdAt = Date(),
                updatedAt = Date(),
                deviceToken = deviceToken,
                title = "title",
                data = mapOf("custom" to "data"),
                isNew = true,
            )

        val events = notification.uncommittedChanges
        assertEquals(1, events.size)
        val event = events.first().payload
        assertTrue(event is PushNotificationCreatedEvent)
        event as PushNotificationCreatedEvent
        assertEquals(id.value.toString(), event.aggregateId)
        assertEquals("body", event.content)
        assertEquals(deviceToken, event.deviceToken)
        assertEquals("title", event.title)
        assertEquals("value", event.payload["key"])
        assertEquals("data", event.data["custom"])
    }

    @Test
    fun `should NOT raise event when isNew is false`() {
        val id = EntityId.generate()
        val deviceToken = DeviceToken.create("token").getOrNull()!!

        val notification =
            PushNotification(
                content = "body",
                payload = emptyMap(),
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                id = id,
                createdAt = Date(),
                updatedAt = Date(),
                deviceToken = deviceToken,
                title = "title",
                data = emptyMap(),
                isNew = false,
            )

        assertTrue(notification.uncommittedChanges.isEmpty())
    }
}
