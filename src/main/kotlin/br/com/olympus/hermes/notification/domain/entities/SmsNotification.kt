package br.com.olympus.hermes.notification.domain.entities

import br.com.olympus.hermes.notification.domain.events.*
import br.com.olympus.hermes.notification.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.Date

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
    private val isNew: Boolean,
) : Notification(
        content = content,
        payload = payload,
        shippingReceipt = shippingReceipt,
        sentAt = sentAt,
        deliveryAt = deliveryAt,
        seenAt = seenAt,
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ) {
    init {
        if (isNew) {
            applyChange(
                SMSNotificationCreatedEvent(
                    aggregateId = id.value.toString(),
                    content = content,
                    payload = payload,
                    from = from,
                    to = to,
                ),
            )
        }
    }
}
