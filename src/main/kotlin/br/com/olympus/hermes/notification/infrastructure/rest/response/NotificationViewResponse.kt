package br.com.olympus.hermes.notification.infrastructure.rest.response

import br.com.olympus.hermes.notification.domain.factories.NotificationType
import br.com.olympus.hermes.notification.infrastructure.readmodel.NotificationView
import java.util.Date

/**
 * HTTP response DTO representing a detailed notification view, including its delivery lifecycle
 * status.
 *
 * @property id The unique identifier of the notification.
 * @property type The notification channel type (EMAIL, SMS, WHATSAPP, PUSH).
 * @property status The current status of the notification (PENDING, SENT, FAILED).
 * @property failureReason The reason for failure if status is FAILED.
 * @property createdAt Timestamp when the notification was created.
 * @property updatedAt Timestamp when the notification was last updated.
 * @property sentAt Timestamp when the notification was successfully sent.
 */
data class NotificationViewResponse(
    val id: String,
    val type: NotificationType,
    val status: String,
    val failureReason: String?,
    val createdAt: Date,
    val updatedAt: Date,
    val sentAt: Date?,
) {
    companion object {
        /**
         * Constructs a [NotificationViewResponse] from a [NotificationView] read-model document.
         *
         * @param view The read-model view to convert.
         * @return The corresponding response DTO.
         */
        fun from(view: NotificationView): NotificationViewResponse =
            NotificationViewResponse(
                id = view.id,
                type = NotificationType.valueOf(view.type),
                status = view.status,
                failureReason = view.failureReason,
                createdAt = view.createdAt,
                updatedAt = view.updatedAt,
                sentAt = view.sentAt,
            )
    }
}
