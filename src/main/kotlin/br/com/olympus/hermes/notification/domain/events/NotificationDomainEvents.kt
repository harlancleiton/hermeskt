package br.com.olympus.hermes.notification.domain.events

import br.com.olympus.hermes.notification.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.notification.domain.valueobjects.Email
import br.com.olympus.hermes.notification.domain.valueobjects.EmailSubject
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import java.util.Date

data class NotificationSentEvent(
    val shippingReceipt: Any,
    val sentAt: Date = Date(),
) : DomainEvent

data class NotificationDeliveryFailedEvent(
    val aggregateId: String,
    val reason: String,
    val failedAt: Date = Date(),
) : DomainEvent

data class NotificationSeenEvent(
    val seenAt: Date = Date(),
) : DomainEvent

data class NotificationDeliveredEvent(
    val deliveredAt: Date = Date(),
) : DomainEvent

sealed interface NotificationCreatedEvent : DomainEvent {
    val aggregateId: String
    val content: String
    val payload: Map<String, Any>
    val type: NotificationType
}

data class EmailNotificationCreatedEvent(
    override val aggregateId: String,
    override val content: String,
    override val payload: Map<String, Any>,
    val from: Email,
    val to: Email,
    val subject: EmailSubject,
) : NotificationCreatedEvent {
    override val type = NotificationType.EMAIL
}

data class SMSNotificationCreatedEvent(
    override val aggregateId: String,
    override val content: String,
    override val payload: Map<String, Any>,
    // TODO add PhoneNumber value object
    val from: UInt,
    val to: BrazilianPhone,
) : NotificationCreatedEvent {
    override val type = NotificationType.SMS
}

data class WhatsAppNotificationCreatedEvent(
    override val aggregateId: String,
    override val content: String,
    override val payload: Map<String, Any>,
    val from: BrazilianPhone,
    val to: BrazilianPhone,
    val templateName: String,
) : NotificationCreatedEvent {
    override val type = NotificationType.WHATSAPP
}

data class PushNotificationCreatedEvent(
    override val aggregateId: String,
    override val content: String,
    override val payload: Map<String, Any>,
    val deviceToken: br.com.olympus.hermes.notification.domain.valueobjects.DeviceToken,
    val title: String,
    val data: Map<String, String>,
) : NotificationCreatedEvent {
    override val type = NotificationType.PUSH
}
