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
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class DeleteTemplateHandler(
    private val templateRepository: TemplateRepository,
) : CommandHandler<DeleteTemplateCommand> {
    @WithSpan("template.delete")
    override fun handle(command: DeleteTemplateCommand): Either<BaseError, Unit> =
        either {
            Span.current().apply {
                setAttribute("template.name.raw", command.name)
                setAttribute("template.channel.raw", command.channel)
            }
            Log.info(
                "Deleting template name=${command.name} channel=${command.channel}",
            )
            val name = TemplateName.create(command.name).bind()
            val channel = parseChannel(command.channel).bind()

            val deleted = templateRepository.deleteByNameAndChannel(name, channel).bind()
            if (!deleted) {
                raise(TemplateNotFoundError(name.value, channel.name))
            }

            Span.current().apply {
                setAttribute("template.deleted", true)
                setAttribute("template.channel", channel.name)
                setAttribute("template.name", name.value)
            }
            Log.info(
                "Template deleted name=${name.value} channel=${channel.name}",
            )
            Unit
        }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
        Either.catch { NotificationType.valueOf(channel.trim().uppercase()) }.mapLeft {
            InvalidPayloadError("Invalid channel: $channel")
        }
}
