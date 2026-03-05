package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.events.PushNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.valueobjects.DeviceToken
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.Date

class PushNotification(
    content: String,
    payload: Map<String, Any>,
    shippingReceipt: Any?,
    sentAt: Date?,
    deliveryAt: Date?,
    seenAt: Date?,
    id: EntityId,
    createdAt: Date,
    updatedAt: Date,
    val deviceToken: DeviceToken,
    val title: String,
    val data: Map<String, String>,
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
                PushNotificationCreatedEvent(
                    aggregateId = id.value.toString(),
                    content = content,
                    payload = payload,
                    deviceToken = deviceToken,
                    title = title,
                    data = data,
                ),
            )
        }
    }
}
