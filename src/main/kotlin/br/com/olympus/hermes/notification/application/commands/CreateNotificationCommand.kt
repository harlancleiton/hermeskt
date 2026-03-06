package br.com.olympus.hermes.notification.application.commands

import br.com.olympus.hermes.notification.domain.factories.CreateNotificationInput
import br.com.olympus.hermes.notification.domain.factories.NotificationType
import br.com.olympus.hermes.shared.application.cqrs.Command
import java.util.UUID

/**
 * Sealed interface representing commands for creating notifications. Each subtype carries the
 * channel-specific data required for notification creation. Fields use raw types (String) so that
 * value object validation happens within the handler boundary.
 */
sealed interface CreateNotificationCommand : Command {
    val id: String
    val type: NotificationType
    val content: String
    val payload: Map<String, Any>
    val templateName: String?

    /**
     * Command for creating an email notification.
     *
     * @property content The email body content.
     * @property payload Additional metadata for template rendering.
     * @property from The sender's email address (raw string, validated in the handler).
     * @property to The recipient's email address (raw string, validated in the handler).
     * @property subject The email subject line (raw string, validated in the handler).
     */
    data class Email(
        override val id: String = UUID.randomUUID().toString(),
        override val type: NotificationType = NotificationType.EMAIL,
        override val content: String,
        override val payload: Map<String, Any> = emptyMap(),
        override val templateName: String? = null,
        val from: String,
        val to: String,
        val subject: String,
    ) : CreateNotificationCommand

    /**
     * Command for creating an SMS notification.
     *
     * @property content The SMS body content.
     * @property payload Additional metadata for template rendering.
     * @property from The sender's short code.
     * @property to The recipient's phone number (raw string, validated in the handler).
     */
    data class Sms(
        override val id: String = UUID.randomUUID().toString(),
        override val type: NotificationType = NotificationType.SMS,
        override val content: String,
        override val payload: Map<String, Any> = emptyMap(),
        override val templateName: String? = null,
        val from: UInt,
        val to: String,
    ) : CreateNotificationCommand

    /**
     * Command for creating a WhatsApp notification.
     *
     * @property content The message body content.
     * @property payload Additional metadata for template parameter rendering.
     * @property from The sender's Brazilian phone number (raw string, validated in the handler).
     * @property to The recipient's Brazilian phone number (raw string, validated in the handler).
     * @property templateName The WhatsApp Business API template name (raw string, validated in the
     * handler).
     */
    data class WhatsApp(
        override val id: String = UUID.randomUUID().toString(),
        override val type: NotificationType = NotificationType.WHATSAPP,
        override val content: String,
        override val payload: Map<String, Any> = emptyMap(),
        override val templateName: String? = null,
        val from: String,
        val to: String,
    ) : CreateNotificationCommand

    /**
     * Command for creating a push notification.
     *
     * @property content The notification body content.
     * @property payload Additional metadata for template rendering.
     * @property deviceToken The recipient's device token (raw string, validated in the handler).
     * @property title The notification title (raw string, validated in the handler).
     * @property data Custom key-value pairs for the push payload.
     */
    data class Push(
        override val id: String = UUID.randomUUID().toString(),
        override val type: NotificationType = NotificationType.PUSH,
        override val content: String,
        override val payload: Map<String, Any> = emptyMap(),
        override val templateName: String? = null,
        val deviceToken: String,
        val title: String,
        val data: Map<String, String> = emptyMap(),
    ) : CreateNotificationCommand

    fun toInput(): CreateNotificationInput =
        when (this) {
            is CreateNotificationCommand.Email ->
                CreateNotificationInput.Email(
                    id = id,
                    content = content,
                    payload = payload,
                    from = from,
                    to = to,
                    subject = subject,
                )
            is CreateNotificationCommand.Sms ->
                CreateNotificationInput.Sms(
                    id = id,
                    content = content,
                    payload = payload,
                    from = from,
                    to = to,
                )
            is CreateNotificationCommand.WhatsApp ->
                CreateNotificationInput.WhatsApp(
                    id = id,
                    content = content,
                    payload = payload,
                    from = from,
                    to = to,
                    templateName = templateName ?: "",
                )
            is CreateNotificationCommand.Push ->
                CreateNotificationInput.Push(
                    id = id,
                    content = content,
                    payload = payload,
                    deviceToken = deviceToken,
                    title = title,
                    data = data,
                )
        }
}
