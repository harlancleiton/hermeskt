package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.events.*
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

abstract class Notification
protected constructor(
        val content: String,
        val payload: Map<String, Any>,
        // TODO add interface or value object
        var shippingReceipt: Any?,
        var sentAt: Date?,
        var deliveryAt: Date?,
        var seenAt: Date?,
        id: EntityId,
        createdAt: Date,
        updatedAt: Date
) : AggregateRoot(id, createdAt, updatedAt) {
    fun markAsSent(shippingReceipt: Any) {
        applyChange(
                NotificationSentEvent(EntityId.generate(), id, version, Date(), shippingReceipt)
        )
    }

    fun markAsDelivered() {
        applyChange(NotificationDeliveredEvent(EntityId.generate(), id, version, Date()))
    }

    override fun apply(event: DomainEvent) {
        when (event) {
            is NotificationSeenEvent -> seenAt = event.occurredAt
            is NotificationDeliveredEvent -> deliveryAt = event.occurredAt
            is NotificationSentEvent -> {
                shippingReceipt = event.shippingReceipt
                sentAt = event.occurredAt
            }
            is NotificationCreatedEvent -> {}
        }
    }
}
