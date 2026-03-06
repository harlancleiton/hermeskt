package br.com.olympus.hermes.infrastructure.rest.request

import arrow.core.Either
import arrow.core.right
import br.com.olympus.hermes.core.application.commands.CreateNotificationCommand
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 * HTTP request body DTO for creating a WhatsApp notification.
 *
 * @property content The message body content.
 * @property payload Additional metadata for template parameter rendering.
 * @property from The sender's Brazilian phone number (raw string, validated by the domain).
 * @property to The recipient's Brazilian phone number (raw string, validated by the domain).
 * @property templateName The WhatsApp Business API template name (required by the WhatsApp
 * channel).
 * @property notificationTemplateName Optional Hermes template name for content resolution (F2
 * Template Engine integration).
 */
data class CreateWhatsAppNotificationRequest(
    @Schema(
        description =
            "The message body content. Required when 'notificationTemplateName' is not provided.",
    )
    val content: String? = null,
    @Schema(description = "The sender's Brazilian phone number", required = true)
    val from: String,
    @Schema(description = "The recipient's Brazilian phone number", required = true)
    val to: String,
    @Schema(description = "The WhatsApp Business API template name", required = true)
    val templateName: String,
    @Schema(description = "Optional additional metadata for template parameter rendering")
    val payload: Map<String, Any> = emptyMap(),
    @Schema(
        description =
            "Optional Hermes template name for content resolution (F2 integration)",
    )
    val notificationTemplateName: String? = null,
) {
    /**
     * Converts this HTTP request into a [CreateNotificationCommand.WhatsApp] command.
     *
     * Validation happens at the domain/factory level; this mapping always succeeds.
     *
     * @return A [Right] wrapping the command.
     */
    fun toCommand(): Either<BaseError, CreateNotificationCommand.WhatsApp> =
        CreateNotificationCommand
            .WhatsApp(
                content = content ?: "",
                payload = payload,
                from = from,
                to = to,
                templateName = templateName,
            ).right()
}
