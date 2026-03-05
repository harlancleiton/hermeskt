package br.com.olympus.hermes.core.application.commands

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.CommandHandler
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPayloadError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateNotFoundError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.repositories.TemplateRepository
import br.com.olympus.hermes.shared.domain.valueobjects.TemplateName
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class DeleteTemplateHandler(
    private val templateRepository: TemplateRepository,
) : CommandHandler<DeleteTemplateCommand> {
    override fun handle(command: DeleteTemplateCommand): Either<BaseError, Unit> =
        either {
            val name = TemplateName.create(command.name).bind()
            val channel = parseChannel(command.channel).bind()

            val deleted = templateRepository.deleteByNameAndChannel(name, channel).bind()
            if (!deleted) {
                raise(TemplateNotFoundError(name.value, channel.name))
            }

            Unit
        }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
        Either.catch { NotificationType.valueOf(channel.trim().uppercase()) }.mapLeft {
            InvalidPayloadError("Invalid channel: $channel")
        }
}
