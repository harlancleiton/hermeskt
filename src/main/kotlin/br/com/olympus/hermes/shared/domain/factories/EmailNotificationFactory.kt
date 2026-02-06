package br.com.olympus.hermes.shared.domain.factories

import br.com.olympus.hermes.shared.domain.entities.EmailNotification
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

class EmailNotificationFactory(
    private val from: Email,
    private val to: Email,
    private val subject: EmailSubject
) : NotificationFactory<EmailNotification> {

    override fun create(
        content: String,
        payload: Map<String, Any>,
        id: EntityId?,
        createdAt: Date?
    ): EmailNotification {
        val now = Date()
        return EmailNotification(
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
            subject = subject,
            isNew = true
        )
    }

    override fun reconstitute(events: List<DomainEvent>): EmailNotification {
        require(events.isNotEmpty()) { "Cannot reconstitute entity without events" }

        val creationEvent = events.filterIsInstance<EmailNotificationCreatedEvent>().firstOrNull()
            ?: throw IllegalArgumentException("Event history must contain EmailNotificationCreatedEvent")

        val notification = EmailNotification(
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
            subject = creationEvent.subject,
            isNew = false
        )

        notification.loadFromHistory(events)

        return notification
    }
}

