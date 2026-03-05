package br.com.olympus.hermes.infrastructure.rest.response

import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
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
         * Constructs a [NotificationResponse] from a [NotificationView] read-model document.
         *
         * @param view The read-model view to convert.
         * @return The corresponding response DTO.
         */
        fun from(view: NotificationView): NotificationResponse =
            NotificationResponse(
                id = view.id,
                type = NotificationType.valueOf(view.type),
                createdAt = view.createdAt,
                updatedAt = view.updatedAt,
            )
    }
}
