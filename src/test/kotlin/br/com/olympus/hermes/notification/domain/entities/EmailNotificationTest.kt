package br.com.olympus.hermes.notification.domain.entities

import br.com.olympus.hermes.notification.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.notification.domain.valueobjects.Email
import br.com.olympus.hermes.notification.domain.valueobjects.EmailSubject
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date

class EmailNotificationTest {
    @Test
    fun `should raise EmailNotificationCreatedEvent when isNew is true`() {
        val id = EntityId.generate()
        val from = Email.from("sender@example.com").getOrNull()!!
        val to = Email.from("recipient@example.com").getOrNull()!!
        val subject = EmailSubject.create("Hello!").getOrNull()!!

        val notification =
            EmailNotification(
                content = "Email body",
                payload = mapOf("name" to "Alice"),
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                id = id,
                createdAt = Date(),
                updatedAt = Date(),
                from = from,
                to = to,
                subject = subject,
                isNew = true,
            )

        val events = notification.uncommittedChanges
        assertEquals(1, events.size)
        val event = events.first().payload
        assertTrue(event is EmailNotificationCreatedEvent)
        event as EmailNotificationCreatedEvent
        assertEquals(id.value.toString(), event.aggregateId)
        assertEquals("Email body", event.content)
        assertEquals(from, event.from)
        assertEquals(to, event.to)
        assertEquals(subject, event.subject)
        assertEquals("Alice", event.payload["name"])
    }

    @Test
    fun `should NOT raise event when isNew is false`() {
        val id = EntityId.generate()
        val from = Email.from("sender@example.com").getOrNull()!!
        val to = Email.from("recipient@example.com").getOrNull()!!
        val subject = EmailSubject.create("Hello!").getOrNull()!!

        val notification =
            EmailNotification(
                content = "Email body",
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
                subject = subject,
                isNew = false,
            )

        assertTrue(notification.uncommittedChanges.isEmpty())
    }
}
