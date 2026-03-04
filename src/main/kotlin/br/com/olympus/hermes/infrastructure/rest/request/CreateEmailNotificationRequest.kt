package br.com.olympus.hermes.infrastructure.rest.request

import br.com.olympus.hermes.core.application.commands.CreateNotificationCommand

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
    val content: String,
    val payload: Map<String, Any> = emptyMap(),
    val from: String,
    val to: String,
    val subject: String,
) {
    /**
     * Converts this request DTO into a [CreateNotificationCommand.Email] command.
     *
     * @return The corresponding command.
     */
    fun toCommand(): CreateNotificationCommand.Email =
        CreateNotificationCommand.Email(
            content = content,
            payload = payload,
            from = from,
            to = to,
            subject = subject,
        )
}
