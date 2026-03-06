package br.com.olympus.hermes.template.application.commands

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.CommandHandler
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPayloadError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateNotFoundError
import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

/**
 * Handles the [DeleteTemplateCommand] by verifying the template exists and then deleting it.
 *
 * @param templateRepository Port for accessing and deleting templates.
 */
@ApplicationScoped
class DeleteTemplateHandler(
    private val templateRepository: TemplateRepository,
) : CommandHandler<DeleteTemplateCommand> {
    @WithSpan("template.command.delete")
    override fun handle(command: DeleteTemplateCommand): Either<BaseError, Unit> =
        either {
            Span.current().apply {
                setAttribute("template.name", command.name)
                setAttribute("template.channel", command.channel)
            }
            Log.info("Deleting template name=${command.name} channel=${command.channel}")

            val name = TemplateName.create(command.name).bind()
            val channel = parseChannel(command.channel).bind()

            val exists = templateRepository.existsByNameAndChannel(name, channel).bind()
            if (!exists) raise(TemplateNotFoundError(command.name, command.channel))

            templateRepository.deleteByNameAndChannel(name, channel).bind()
            Log.info("Template deleted successfully name=${command.name} channel=${command.channel}")
        }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
        Either.catch { NotificationType.valueOf(channel.trim().uppercase()) }.mapLeft {
            InvalidPayloadError("Invalid channel: $channel")
        }
}
