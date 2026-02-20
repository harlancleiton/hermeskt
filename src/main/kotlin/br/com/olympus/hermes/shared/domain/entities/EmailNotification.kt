package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

class EmailNotification(
        content: String,
        payload: Map<String, Any>,
        shippingReceipt: Any?,
        sentAt: Date?,
        deliveryAt: Date?,
        seenAt: Date?,
        id: EntityId,
        createdAt: Date,
        updatedAt: Date,
        val from: Email,
        val to: Email,
        val subject: EmailSubject,
        private val isNew: Boolean
) :
        Notification(
                content = content,
                payload = payload,
                shippingReceipt = shippingReceipt,
                sentAt = sentAt,
                deliveryAt = deliveryAt,
                seenAt = seenAt,
                id = id,
                createdAt = createdAt,
                updatedAt = updatedAt
        ) {
    init {
        if (isNew) {
            applyChange(
                    EmailNotificationCreatedEvent(
                            EntityId.generate(),
                            id,
                            version,
                            createdAt,
                            content,
                            payload,
                            from,
                            to,
                            subject
                    )
            )
        }
    }
}
