package br.com.olympus.hermes.core.application.commands

import br.com.olympus.hermes.shared.application.cqrs.Command
import br.com.olympus.hermes.shared.domain.factories.CreateNotificationInput
import br.com.olympus.hermes.shared.domain.factories.NotificationType

/**
 * Sealed interface representing commands for creating notifications. Each subtype carries the
 * channel-specific data required for notification creation. Fields use raw types (String) so that
 * value object validation happens within the handler boundary.
 */
sealed interface CreateNotificationCommand : Command {
        val type: NotificationType
        val content: String
        val payload: Map<String, Any>

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
                override val type: NotificationType = NotificationType.EMAIL,
                override val content: String,
                override val payload: Map<String, Any> = emptyMap(),
                val from: String,
                val to: String,
                val subject: String
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
                override val type: NotificationType = NotificationType.SMS,
                override val content: String,
                override val payload: Map<String, Any> = emptyMap(),
                val from: UInt,
                val to: String
        ) : CreateNotificationCommand

        /**
         * Command for creating a WhatsApp notification.
         *
         * @property content The message body content.
         * @property payload Additional metadata for template parameter rendering.
         * @property from The sender's Brazilian phone number (raw string, validated in the
         * handler).
         * @property to The recipient's Brazilian phone number (raw string, validated in the
         * handler).
         * @property templateName The WhatsApp Business API template name (raw string, validated in
         * the handler).
         */
        data class WhatsApp(
                override val type: NotificationType = NotificationType.WHATSAPP,
                override val content: String,
                override val payload: Map<String, Any> = emptyMap(),
                val from: String,
                val to: String,
                val templateName: String
        ) : CreateNotificationCommand

        fun toInput(): CreateNotificationInput =
                when (this) {
                        is CreateNotificationCommand.Email ->
                                CreateNotificationInput.Email(
                                        content = content,
                                        payload = payload,
                                        from = from,
                                        to = to,
                                        subject = subject
                                )
                        is CreateNotificationCommand.Sms ->
                                CreateNotificationInput.Sms(
                                        content = content,
                                        payload = payload,
                                        from = from,
                                        to = to
                                )
                        is CreateNotificationCommand.WhatsApp ->
                                CreateNotificationInput.WhatsApp(
                                        content = content,
                                        payload = payload,
                                        from = from,
                                        to = to,
                                        templateName = templateName
                                )
                }
}
