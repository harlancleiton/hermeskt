package br.com.olympus.hermes.shared.domain.factories

import br.com.olympus.hermes.shared.domain.entities.SmsNotification
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

class SmsNotificationFactory(
    private val from: UInt,
    private val to: BrazilianPhone
) : NotificationFactory<SmsNotification> {

    override fun create(
        content: String,
        payload: Map<String, Any>,
        id: EntityId?,
        createdAt: Date?
    ): SmsNotification {
        val now = Date()
        return SmsNotification(
            content = content,
            payload = payload,
            shippingReceipt = null,
            sentAt = null,
            deliveryAt = null,
            seenAt = null,
            id = id ?: EntityId.generate(),
            createdAt = createdAt ?: now,
            updatedAt = now,
            from = from,
            to = to,
            isNew = true
        )
    }

    override fun reconstitute(events: List<DomainEvent>): SmsNotification {
        require(events.isNotEmpty()) { "Cannot reconstitute entity without events" }

        val creationEvent = events.filterIsInstance<SMSNotificationCreatedEvent>().firstOrNull()
            ?: throw IllegalArgumentException("Event history must contain SMSNotificationCreatedEvent")

        val notification = SmsNotification(
            content = creationEvent.content,
            payload = creationEvent.payload,
            shippingReceipt = null,
            sentAt = null,
            deliveryAt = null,
            seenAt = null,
            id = creationEvent.aggregateId,
            createdAt = creationEvent.occurredAt,
            updatedAt = creationEvent.occurredAt,
            from = creationEvent.from,
            to = creationEvent.to,
            isNew = false
        )

        notification.loadFromHistory(events)

        return notification
    }
}

