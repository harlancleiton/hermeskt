package br.com.olympus.hermes.template.application.commands

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.CommandHandler
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPayloadError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateNotFoundError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import br.com.olympus.hermes.template.domain.valueobjects.TemplateBody
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

/**
 * Handles the [UpdateTemplateCommand] by fetching the existing template, applying updates, and
 * persisting the result.
 *
 * @param templateRepository Port for accessing and updating templates.
 */
@ApplicationScoped
class UpdateTemplateHandler(
        private val templateRepository: TemplateRepository,
) : CommandHandler<UpdateTemplateCommand> {

    @WithSpan("template.command.update")
    override fun handle(command: UpdateTemplateCommand): Either<BaseError, Unit> = either {
        Span.current().apply {
            setAttribute("template.name", command.name)
            setAttribute("template.channel", command.channel)
        }
        Log.info("Updating template name=${command.name} channel=${command.channel}")

        val name = TemplateName.create(command.name).bind()
        val channel = parseChannel(command.channel).bind()

        val existing =
                templateRepository.findByNameAndChannel(name, channel).bind()
                        ?: raise(TemplateNotFoundError(command.name, command.channel))

        val newBody = command.body?.let { TemplateBody.create(it).bind() } ?: existing.body

        val updated =
                existing.copy(
                        subject = command.subject ?: existing.subject,
                        body = newBody,
                        description = command.description ?: existing.description,
                )
        templateRepository.update(updated).bind()
        Log.info("Template updated successfully name=${command.name} channel=${command.channel}")
    }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
            Either.catch { NotificationType.valueOf(channel.trim().uppercase()) }.mapLeft {
                InvalidPayloadError("Invalid channel: $channel")
            }
}
