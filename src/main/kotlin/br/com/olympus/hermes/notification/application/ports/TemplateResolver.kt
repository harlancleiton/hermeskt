package br.com.olympus.hermes.notification.application.ports

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.BaseError

/**
 * DTO representing a resolved template with its subject and body.
 *
 * @property subject The resolved subject (usually for emails).
 * @property body The resolved body content.
 */
data class ResolvedTemplateDto(
    val subject: String?,
    val body: String,
)

/**
 * Port interface for resolving templates across context boundaries. This decouples the notification
 * context from the template engine's internal details.
 */
interface TemplateResolver {
    /**
     * Resolves a template by its name, channel, and payload variables.
     *
     * @param templateName The primitive name of the template.
     * @param channel The notification channel/type.
     * @param payload The variables for interpolation.
     * @return Either a [BaseError] or the [ResolvedTemplateDto].
     */
    fun resolve(
        templateName: String,
        channel: NotificationType,
        payload: Map<String, Any>,
    ): Either<BaseError, ResolvedTemplateDto>
}
