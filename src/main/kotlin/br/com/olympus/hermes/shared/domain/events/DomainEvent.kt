package br.com.olympus.hermes.shared.domain.events

import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
import java.util.Date

interface DomainEvent

data class NotificationSentEvent(
    val shippingReceipt: Any,
    val sentAt: Date = Date(),
) : DomainEvent

data class NotificationSeenEvent(
    val seenAt: Date = Date(),
) : DomainEvent

data class NotificationDeliveredEvent(
    val deliveredAt: Date = Date(),
) : DomainEvent

sealed interface NotificationCreatedEvent : DomainEvent {
    val content: String
    val payload: Map<String, Any>
    val type: NotificationType
}

data class EmailNotificationCreatedEvent(
    override val content: String,
    override val payload: Map<String, Any>,
    val from: Email,
    val to: Email,
    val subject: EmailSubject,
) : NotificationCreatedEvent {
    override val type = NotificationType.EMAIL
}

data class SMSNotificationCreatedEvent(
    override val content: String,
    override val payload: Map<String, Any>,
    // TODO add PhoneNumber value object
    val from: UInt,
    val to: BrazilianPhone,
) : NotificationCreatedEvent {
    override val type = NotificationType.SMS
}

data class WhatsAppNotificationCreatedEvent(
    override val content: String,
    override val payload: Map<String, Any>,
    val from: BrazilianPhone,
    val to: BrazilianPhone,
    val templateName: String,
) : NotificationCreatedEvent {
    override val type = NotificationType.WHATSAPP
}
