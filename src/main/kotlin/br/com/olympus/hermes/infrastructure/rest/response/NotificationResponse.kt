package br.com.olympus.hermes.infrastructure.rest.response

import br.com.olympus.hermes.shared.domain.entities.EmailNotification
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.entities.SmsNotification
import br.com.olympus.hermes.shared.domain.entities.WhatsAppNotification
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import java.util.Date

/**
 * HTTP response DTO representing a created or retrieved notification.
 *
 * @property id The unique identifier of the notification.
 * @property type The notification channel type (EMAIL, SMS, WHATSAPP).
 * @property createdAt Timestamp when the notification was created.
 * @property updatedAt Timestamp when the notification was last updated.
 */
data class NotificationResponse(
    val id: String,
    val type: NotificationType,
    val createdAt: Date,
    val updatedAt: Date,
) {
    companion object {
        /**
         * Constructs a [NotificationResponse] from a [Notification] domain entity.
         *
         * @param notification The domain entity to convert.
         * @return The corresponding response DTO.
         */
        fun from(notification: Notification): NotificationResponse =
            NotificationResponse(
                id = notification.id.value.toString(),
                type =
                    when (notification) {
                        is EmailNotification -> NotificationType.EMAIL
                        is SmsNotification -> NotificationType.SMS
                        is WhatsAppNotification -> NotificationType.WHATSAPP
                        else ->
                            error(
                                "Unknown notification subtype: ${notification::class.simpleName}",
                            )
                    },
                createdAt = notification.createdAt,
                updatedAt = notification.updatedAt,
            )
    }
}
