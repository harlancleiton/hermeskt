package br.com.olympus.hermes.template.application.queries

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.QueryHandler
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPayloadError
import br.com.olympus.hermes.template.domain.entities.NotificationTemplate
import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

/**
 * Handles the [GetTemplateQuery] by querying the template repository for a specific template.
 *
 * @param templateRepository Port for accessing templates.
 */
@ApplicationScoped
class GetTemplateQueryHandler(
    private val templateRepository: TemplateRepository,
) : QueryHandler<GetTemplateQuery, NotificationTemplate?> {
    @WithSpan("template.query.get")
    override fun handle(query: GetTemplateQuery): Either<BaseError, NotificationTemplate?> =
        either {
            Span.current().apply {
                setAttribute("template.name", query.name)
                setAttribute("template.channel", query.channel)
            }
            Log.debug("Querying template name=${query.name} channel=${query.channel}")
            val name = TemplateName.create(query.name).bind()
            val channel = parseChannel(query.channel).bind()
            val template = templateRepository.findByNameAndChannel(name, channel).bind()
            Span.current().setAttribute("template.found", template != null)
            template
        }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
        Either.catch { NotificationType.valueOf(channel.trim().uppercase()) }.mapLeft {
            InvalidPayloadError("Invalid channel: $channel")
        }
}
