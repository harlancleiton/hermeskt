package br.com.olympus.hermes.infrastructure.rest.request

import arrow.core.Either
import arrow.core.right
import br.com.olympus.hermes.core.application.commands.CreateNotificationCommand
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 * HTTP request body DTO for creating an SMS notification.
 *
 * @property content The SMS body content.
 * @property payload Additional metadata for template rendering.
 * @property from The sender's short code (UInt).
 * @property to The recipient's phone number (raw string, validated by the domain).
 * @property templateName Optional Hermes template name for content resolution (F2 integration).
 */
data class CreateSmsNotificationRequest(
    @Schema(description = "The SMS body content", required = true) val content: String,
    @Schema(description = "The sender's short code") val from: UInt,
    @Schema(description = "The recipient's phone number") val to: String,
    @Schema(description = "Optional additional metadata for tracking/domain")
    val payload: Map<String, Any> = emptyMap(),
    @Schema(
        description =
            "Optional Hermes template name for content resolution (F2 integration)",
    )
    val templateName: String? = null,
) {
    /**
     * Converts this HTTP request into a [CreateNotificationCommand.Sms] command.
     *
     * Validation happens at the domain/factory level; this mapping always succeeds.
     *
     * @return A [Right] wrapping the command.
     */
    fun toCommand(): Either<BaseError, CreateNotificationCommand.Sms> =
        CreateNotificationCommand
            .Sms(
                content = content,
                payload = payload,
                from = from,
                to = to,
                templateName = templateName,
            ).right()
}
