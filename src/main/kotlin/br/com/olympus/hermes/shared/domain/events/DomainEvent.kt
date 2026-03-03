package br.com.olympus.hermes.shared.domain.events

import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.Date

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
    val shippingReceipt: Any,
) : DomainEvent {
    override val aggregateType = "Notification"
    override val eventType = "NotificationSentEvent"
}

data class NotificationSeenEvent(
    override val id: EntityId,
    override val aggregateId: EntityId,
    override val aggregateVersion: Int,
    override val occurredAt: Date,
) : DomainEvent {
    override val aggregateType = "Notification"
    override val eventType = "NotificationSeenEvent"
}

data class NotificationDeliveredEvent(
    override val id: EntityId,
    override val aggregateId: EntityId,
    override val aggregateVersion: Int,
    override val occurredAt: Date,
) : DomainEvent {
    override val aggregateType = "Notification"
    override val eventType = "NotificationDeliveredEvent"
}

sealed interface NotificationCreatedEvent : DomainEvent {
    val content: String
    val payload: Map<String, Any>
    val type: NotificationType
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
    val subject: EmailSubject,
) : NotificationCreatedEvent {
    override val aggregateType = "Notification"
    override val eventType = "EmailNotificationCreatedEvent"
    override val type = NotificationType.EMAIL
}

data class SMSNotificationCreatedEvent(
    override val id: EntityId,
    override val aggregateId: EntityId,
    override val aggregateVersion: Int,
    override val occurredAt: Date,
    override val content: String,
    override val payload: Map<String, Any>,
    // TODO add PhoneNumber value object
    val from: UInt,
    val to: BrazilianPhone,
) : NotificationCreatedEvent {
    override val aggregateType = "Notification"
    override val eventType = "SMSNotificationCreatedEvent"
    override val type = NotificationType.SMS
}

data class WhatsAppNotificationCreatedEvent(
    override val id: EntityId,
    override val aggregateId: EntityId,
    override val aggregateVersion: Int,
    override val occurredAt: Date,
    override val content: String,
    override val payload: Map<String, Any>,
    val from: BrazilianPhone,
    val to: BrazilianPhone,
    val templateName: String,
) : NotificationCreatedEvent {
    override val aggregateType = "Notification"
    override val eventType = "WhatsAppNotificationCreatedEvent"
    override val type = NotificationType.WHATSAPP
}
