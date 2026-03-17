package br.com.olympus.hermes.notification.domain.entities

import br.com.olympus.hermes.notification.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.notification.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date

class SmsNotificationTest {
    @Test
    fun `should raise SMSNotificationCreatedEvent when isNew is true`() {
        val id = EntityId.generate()
        val to = BrazilianPhone.create("11999887766").getOrNull()!!

        val notification =
            SmsNotification(
                content = "Your OTP is 123456",
                payload = mapOf("code" to "123456"),
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                id = id,
                createdAt = Date(),
                updatedAt = Date(),
                from = 12345u,
                to = to,
                isNew = true,
            )

        val events = notification.uncommittedChanges
        assertEquals(1, events.size)
        val event = events.first().payload
        assertTrue(event is SMSNotificationCreatedEvent)
        event as SMSNotificationCreatedEvent
        assertEquals(id.value.toString(), event.aggregateId)
        assertEquals("Your OTP is 123456", event.content)
        assertEquals(12345u, event.from)
        assertEquals(to, event.to)
        assertEquals("123456", event.payload["code"])
    }

    @Test
    fun `should NOT raise event when isNew is false`() {
        val id = EntityId.generate()
        val to = BrazilianPhone.create("11999887766").getOrNull()!!

        val notification =
            SmsNotification(
                content = "Your OTP is 123456",
                payload = emptyMap(),
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                id = id,
                createdAt = Date(),
                updatedAt = Date(),
                from = 12345u,
                to = to,
                isNew = false,
            )

        assertTrue(notification.uncommittedChanges.isEmpty())
    }
}
