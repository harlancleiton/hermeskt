package br.com.olympus.hermes.core.application.queries

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.QueryHandler
import br.com.olympus.hermes.shared.domain.entities.NotificationTemplate
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPayloadError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.repositories.TemplateRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ListTemplatesQueryHandler(
    private val templateRepository: TemplateRepository,
) : QueryHandler<ListTemplatesQuery, List<NotificationTemplate>> {
    override fun handle(query: ListTemplatesQuery): Either<BaseError, List<NotificationTemplate>> =
        either {
            val channel = query.channel?.let { parseChannel(it).bind() }
            templateRepository.findAllByChannel(channel, query.page, query.size).bind()
        }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
        Either
            .catch { NotificationType.valueOf(channel.trim().uppercase()) }
            .mapLeft { InvalidPayloadError("Invalid channel: $channel") }
}
