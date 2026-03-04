package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.events.NotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.NotificationDeliveredEvent
import br.com.olympus.hermes.shared.domain.events.NotificationSeenEvent
import br.com.olympus.hermes.shared.domain.events.NotificationSentEvent
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.Date

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
        updatedAt: Date,
    ) : AggregateRoot(id, createdAt, updatedAt) {
        fun markAsSent(shippingReceipt: Any) {
            applyChange(NotificationSentEvent(shippingReceipt = shippingReceipt, sentAt = Date()))
        }

        fun markAsDelivered() {
            applyChange(NotificationDeliveredEvent(deliveredAt = Date()))
        }

        override fun apply(event: DomainEvent) {
            when (event) {
                is NotificationSeenEvent -> seenAt = event.seenAt
                is NotificationDeliveredEvent -> deliveryAt = event.deliveredAt
                is NotificationSentEvent -> {
                    shippingReceipt = event.shippingReceipt
                    sentAt = event.sentAt
                }
                is NotificationCreatedEvent -> {}
            }
        }
    }
