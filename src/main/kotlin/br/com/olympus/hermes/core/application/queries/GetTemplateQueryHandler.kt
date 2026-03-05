package br.com.olympus.hermes.core.application.queries

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.QueryHandler
import br.com.olympus.hermes.shared.domain.entities.NotificationTemplate
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPayloadError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.repositories.TemplateRepository
import br.com.olympus.hermes.shared.domain.valueobjects.TemplateName
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class GetTemplateQueryHandler(
    private val templateRepository: TemplateRepository,
) : QueryHandler<GetTemplateQuery, NotificationTemplate?> {
    override fun handle(query: GetTemplateQuery): Either<BaseError, NotificationTemplate?> =
        either {
            val name = TemplateName.create(query.name).bind()
            val channel = parseChannel(query.channel).bind()
            templateRepository.findByNameAndChannel(name, channel).bind()
        }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
        Either
            .catch { NotificationType.valueOf(channel.trim().uppercase()) }
            .mapLeft { InvalidPayloadError("Invalid channel: $channel") }
}
