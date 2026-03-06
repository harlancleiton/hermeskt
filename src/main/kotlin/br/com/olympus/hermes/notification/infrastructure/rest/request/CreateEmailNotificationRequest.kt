package br.com.olympus.hermes.notification.infrastructure.rest.request

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import br.com.olympus.hermes.notification.application.commands.CreateNotificationCommand
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPayloadError

/**
 * HTTP request body DTO for creating an email notification.
 *
 * @property content The email body content.
 * @property payload Additional metadata for template rendering.
 * @property from The sender's email address.
 * @property to The recipient's email address.
 * @property subject The email subject line.
 */
data class CreateEmailNotificationRequest(
    val content: String? = null,
    val templateName: String? = null,
    val payload: Map<String, Any> = emptyMap(),
    val from: String,
    val to: String,
    val subject: String? = null,
) {
    /**
     * Converts this request DTO into a [CreateNotificationCommand.Email] command.
     *
     * @return The corresponding command.
     */
    fun toCommand(): Either<BaseError, CreateNotificationCommand.Email> =
        either {
            val hasContent = !content.isNullOrBlank() && !subject.isNullOrBlank()
            val hasTemplate = !templateName.isNullOrBlank()

            ensure(hasContent || hasTemplate) {
                InvalidPayloadError(
                    "Either 'content' and 'subject' or 'templateName' must be provided",
                )
            }

            CreateNotificationCommand.Email(
                content = content ?: "",
                templateName = templateName,
                payload = payload,
                from = from,
                to = to,
                subject = subject ?: "",
            )
        }
}
