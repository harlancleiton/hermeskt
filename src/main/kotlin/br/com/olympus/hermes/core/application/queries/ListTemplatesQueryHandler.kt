package br.com.olympus.hermes.core.application.queries

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.QueryHandler
import br.com.olympus.hermes.shared.domain.entities.NotificationTemplate
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPayloadError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.repositories.TemplateRepository
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ListTemplatesQueryHandler(
    private val templateRepository: TemplateRepository,
) : QueryHandler<ListTemplatesQuery, List<NotificationTemplate>> {
    @WithSpan("template.query.list")
    override fun handle(query: ListTemplatesQuery): Either<BaseError, List<NotificationTemplate>> =
        either {
            Span.current().apply {
                setAttribute("template.channel.filter", query.channel ?: "<all>")
                setAttribute("query.page", query.page.toLong())
                setAttribute("query.size", query.size.toLong())
            }
            Log.debugf(
                "Listing templates channel=%s page=%d size=%d",
                query.channel ?: "<all>",
                query.page,
                query.size,
            )
            val channel = query.channel?.let { parseChannel(it).bind() }
            val templates =
                templateRepository.findAllByChannel(channel, query.page, query.size).bind()
            Span.current().setAttribute("template.result.count", templates.size.toLong())
            templates
        }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
        Either.catch { NotificationType.valueOf(channel.trim().uppercase()) }.mapLeft {
            InvalidPayloadError("Invalid channel: $channel")
        }
}
