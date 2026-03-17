package br.com.olympus.hermes.notification.domain.entities

import br.com.olympus.hermes.notification.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.notification.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date

class WhatsAppNotificationTest {
    @Test
    fun `should raise WhatsAppNotificationCreatedEvent when isNew is true`() {
        val id = EntityId.generate()
        val from = BrazilianPhone.create("11888888888").getOrNull()!!
        val to = BrazilianPhone.create("11999887766").getOrNull()!!

        val notification =
            WhatsAppNotification(
                content = "Your code is {{code}}",
                payload = mapOf("code" to "999888"),
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                id = id,
                createdAt = Date(),
                updatedAt = Date(),
                from = from,
                to = to,
                templateName = "otp-template",
                isNew = true,
            )

        val events = notification.uncommittedChanges
        assertEquals(1, events.size)
        val event = events.first().payload
        assertTrue(event is WhatsAppNotificationCreatedEvent)
        event as WhatsAppNotificationCreatedEvent
        assertEquals(id.value.toString(), event.aggregateId)
        assertEquals("Your code is {{code}}", event.content)
        assertEquals(from, event.from)
        assertEquals(to, event.to)
        assertEquals("otp-template", event.templateName)
        assertEquals("999888", event.payload["code"])
    }

    @Test
    fun `should NOT raise event when isNew is false`() {
        val id = EntityId.generate()
        val from = BrazilianPhone.create("11888888888").getOrNull()!!
        val to = BrazilianPhone.create("11999887766").getOrNull()!!

        val notification =
            WhatsAppNotification(
                content = "template body",
                payload = emptyMap(),
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                id = id,
                createdAt = Date(),
                updatedAt = Date(),
                from = from,
                to = to,
                templateName = "otp-template",
                isNew = false,
            )

        assertTrue(notification.uncommittedChanges.isEmpty())
    }
}
