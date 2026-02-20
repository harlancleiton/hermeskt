package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

class SmsNotification(
        content: String,
        payload: Map<String, Any>,
        shippingReceipt: Any?,
        sentAt: Date?,
        deliveryAt: Date?,
        seenAt: Date?,
        id: EntityId,
        createdAt: Date,
        updatedAt: Date,
        // TODO add generic Phone value object
        val from: UInt,
        val to: BrazilianPhone,
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
                    SMSNotificationCreatedEvent(
                            EntityId.generate(),
                            id,
                            version,
                            createdAt,
                            content,
                            payload,
                            from,
                            to
                    )
            )
        }
    }
}
