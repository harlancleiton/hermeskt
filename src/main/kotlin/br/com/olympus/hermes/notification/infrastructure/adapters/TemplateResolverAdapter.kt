package br.com.olympus.hermes.notification.infrastructure.adapters

import arrow.core.Either
import arrow.core.flatMap
import br.com.olympus.hermes.notification.application.ports.ResolvedTemplateDto
import br.com.olympus.hermes.notification.application.ports.TemplateResolver
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.template.domain.services.TemplateEngine
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import jakarta.enterprise.context.ApplicationScoped

/**
 * Adapter implementation of [TemplateResolver] that delegates to the [TemplateEngine] in the
 * template context. This class acts as the bridge between bounded contexts.
 */
@ApplicationScoped
class TemplateResolverAdapter(
    private val templateEngine: TemplateEngine,
) : TemplateResolver {
    override fun resolve(
        templateName: String,
        channel: NotificationType,
        payload: Map<String, Any>,
    ): Either<BaseError, ResolvedTemplateDto> =
        TemplateName
            .create(templateName)
            .flatMap { name: TemplateName -> templateEngine.resolve(name, channel, payload) }
            .map { resolved ->
                ResolvedTemplateDto(
                    subject = resolved.subject,
                    body = resolved.body,
                )
            }
}
