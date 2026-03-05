package br.com.olympus.hermes.infrastructure.rest.request

import arrow.core.Either
import arrow.core.right
import br.com.olympus.hermes.core.application.commands.CreateNotificationCommand
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 * HTTP request body for creating a push notification.
 *
 * @property deviceToken The recipient's device token.
 * @property title The push notification title.
 * @property body The push notification message body.
 * @property payload Optional custom parameters for the domain.
 * @property data Optional custom payload parameters for FCM/APNs.
 */
data class CreatePushNotificationRequest(
    @Schema(description = "The recipient's device token", required = true)
    val deviceToken: String,
    @Schema(description = "The push notification title", required = true)
    val title: String,
    @Schema(description = "The push notification message body", required = true)
    val body: String,
    @Schema(description = "Optional additional metadata for tracking/domain")
    val payload: Map<String, Any> = emptyMap(),
    @Schema(description = "Optional custom push data for FCM/APNs (e.g. deep links)")
    val data: Map<String, String> = emptyMap(),
) {
    /**
     * Converts this HTTP request into a CQRS command. Since validation happens purely at
     * the domain/factory level, this always succeeds (Right).
     */
    fun toCommand(): Either<BaseError, CreateNotificationCommand.Push> =
        CreateNotificationCommand
            .Push(
                deviceToken = deviceToken,
                title = title,
                content = body,
                payload = payload,
                data = data,
            ).right()
}
