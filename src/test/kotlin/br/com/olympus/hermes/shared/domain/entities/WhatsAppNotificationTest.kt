package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WhatsAppNotificationTest {

    private val validFrom = BrazilianPhone.create("11987654321").getOrNull()!!
    private val validTo = BrazilianPhone.create("11912345678").getOrNull()!!
    private val validContent = "Hello, this is a test message"
    private val validTemplateName = "hello_world"

    private fun buildNotification(isNew: Boolean): WhatsAppNotification {
        val now = Date()
        return WhatsAppNotification(
                content = validContent,
                payload = emptyMap(),
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                id = EntityId.generate(),
                createdAt = now,
                updatedAt = now,
                from = validFrom,
                to = validTo,
                templateName = validTemplateName,
                isNew = isNew
        )
    }

    @Test
    fun `constructor_withIsNewTrue_producesCreatedEvent`() {
        val notification = buildNotification(isNew = true)

        val changes = notification.uncommittedChanges
        assertEquals(1, changes.size)
        assertTrue(changes.first() is WhatsAppNotificationCreatedEvent)
        val event = changes.first() as WhatsAppNotificationCreatedEvent
        assertEquals(validContent, event.content)
        assertEquals(validFrom, event.from)
        assertEquals(validTo, event.to)
        assertEquals(validTemplateName, event.templateName)
    }

    @Test
    fun `constructor_withIsNewFalse_producesNoEvents`() {
        val notification = buildNotification(isNew = false)

        assertTrue(notification.uncommittedChanges.isEmpty())
    }

    @Test
    fun `loadFromHistory_reconstitutesState`() {
        val aggregateId = EntityId.generate()
        val now = Date()
        val creationEvent =
                WhatsAppNotificationCreatedEvent(
                        id = EntityId.generate(),
                        aggregateId = aggregateId,
                        aggregateVersion = 0,
                        occurredAt = now,
                        content = validContent,
                        payload = mapOf("key" to "value"),
                        from = validFrom,
                        to = validTo,
                        templateName = validTemplateName
                )

        val notification =
                WhatsAppNotification(
                        content = validContent,
                        payload = mapOf("key" to "value"),
                        shippingReceipt = null,
                        sentAt = null,
                        deliveryAt = null,
                        seenAt = null,
                        id = aggregateId,
                        createdAt = now,
                        updatedAt = now,
                        from = validFrom,
                        to = validTo,
                        templateName = validTemplateName,
                        isNew = false
                )

        notification.loadFromHistory(listOf(creationEvent))

        assertEquals(aggregateId, notification.id)
        assertEquals(validContent, notification.content)
        assertEquals(validFrom, notification.from)
        assertEquals(validTo, notification.to)
        assertEquals(validTemplateName, notification.templateName)
        assertTrue(notification.uncommittedChanges.isEmpty())
    }
}
