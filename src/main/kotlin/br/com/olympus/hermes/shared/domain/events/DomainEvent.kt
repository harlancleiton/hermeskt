package br.com.olympus.hermes.shared.domain.events

import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

sealed interface DomainEvent {
    val id: EntityId
    val aggregateId: EntityId
    val aggregateVersion: Int
    val aggregateType: String
    val eventType: String
    val occurredAt: Date
}

data class NotificationSentEvent(
    override val id: EntityId,
    override val aggregateId: EntityId,
    override val aggregateVersion: Int,
    override val occurredAt: Date,
    val shippingReceipt: Any
) : DomainEvent {
    override val aggregateType = "Notification"
    override val eventType = "NotificationSentEvent"
}

data class NotificationSeenEvent(
    override val id: EntityId,
    override val aggregateId: EntityId,
    override val aggregateVersion: Int,
    override val occurredAt: Date
) : DomainEvent {
    override val aggregateType = "Notification"
    override val eventType = "NotificationSeenEvent"
}

data class NotificationDeliveredEvent(
    override val id: EntityId,
    override val aggregateId: EntityId,
    override val aggregateVersion: Int,
    override val occurredAt: Date
) : DomainEvent {
    override val aggregateType = "Notification"
    override val eventType = "NotificationDeliveredEvent"
}

sealed interface NotificationCreatedEvent
    : DomainEvent {
    val content: String
    val payload: Map<String, Any>
}

data class EmailNotificationCreatedEvent(
    override val id: EntityId,
    override val aggregateId: EntityId,
    override val aggregateVersion: Int,
    override val occurredAt: Date,
    override val content: String,
    override val payload: Map<String, Any>,
    val from: Email,
    val to: Email,
    // TODO add EmailSubject value object
    val subject: String,
) : NotificationCreatedEvent {
    override val aggregateType = "Notification"
    override val eventType = "EmailNotificationCreatedEvent"
}

data class SMSNotificationCreatedEvent(
    override val id: EntityId,
    override val aggregateId: EntityId,
    override val aggregateVersion: Int,
    override val occurredAt: Date,
    override val content: String,
    override val payload: Map<String, Any>,
    // TODO add PhoneNumber value object
    val from: Int,
    val to: Int,
) : NotificationCreatedEvent {
    override val aggregateType = "Notification"
    override val eventType = "SMSNotificationCreatedEvent"
}
