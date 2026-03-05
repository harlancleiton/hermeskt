package br.com.olympus.hermes.core.application.commands

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.CommandHandler
import br.com.olympus.hermes.shared.domain.entities.NotificationTemplate
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPayloadError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateNotFoundError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.repositories.TemplateRepository
import br.com.olympus.hermes.shared.domain.valueobjects.TemplateBody
import br.com.olympus.hermes.shared.domain.valueobjects.TemplateName
import jakarta.enterprise.context.ApplicationScoped
import java.util.Date

@ApplicationScoped
class UpdateTemplateHandler(
    private val templateRepository: TemplateRepository,
) : CommandHandler<UpdateTemplateCommand> {
    override fun handle(command: UpdateTemplateCommand): Either<BaseError, Unit> =
        either {
            val name = TemplateName.create(command.name).bind()
            val channel = parseChannel(command.channel).bind()

            val existing =
                templateRepository.findByNameAndChannel(name, channel).bind()
                    ?: raise(TemplateNotFoundError(name.value, channel.name))

            val updated =
                NotificationTemplate(
                    name = existing.name,
                    channel = existing.channel,
                    subject = command.subject ?: existing.subject,
                    body =
                        command.body?.let { TemplateBody.create(it).bind() }
                            ?: existing.body,
                    description = command.description ?: existing.description,
                    createdAt = existing.createdAt,
                    updatedAt = Date(),
                )

            templateRepository.update(updated).bind()
            Unit
        }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
        Either.catch { NotificationType.valueOf(channel.trim().uppercase()) }.mapLeft {
            InvalidPayloadError("Invalid channel: $channel")
        }
}
