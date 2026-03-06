package br.com.olympus.hermes.template.application.commands

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.CommandHandler
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPayloadError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateDuplicateError
import br.com.olympus.hermes.template.domain.entities.NotificationTemplate
import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import br.com.olympus.hermes.template.domain.valueobjects.TemplateBody
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

/**
 * Handles the [CreateTemplateCommand] by validating inputs, checking for duplicates, and persisting
 * a new [NotificationTemplate].
 *
 * @param templateRepository Port for persisting templates.
 */
@ApplicationScoped
class CreateTemplateHandler(
    private val templateRepository: TemplateRepository,
) : CommandHandler<CreateTemplateCommand> {
    @WithSpan("template.command.create")
    override fun handle(command: CreateTemplateCommand): Either<BaseError, Unit> =
        either {
            Span.current().apply {
                setAttribute("template.name", command.name)
                setAttribute("template.channel", command.channel)
            }
            Log.info("Creating template name=${command.name} channel=${command.channel}")

            val name = TemplateName.create(command.name).bind()
            val channel = parseChannel(command.channel).bind()
            val body = TemplateBody.create(command.body).bind()

            val exists = templateRepository.existsByNameAndChannel(name, channel).bind()
            if (exists) raise(TemplateDuplicateError(command.name, command.channel))

            val template =
                NotificationTemplate(
                    name = name,
                    channel = channel,
                    subject = command.subject,
                    body = body,
                    description = command.description,
                )
            templateRepository.save(template).bind()
            Log.info("Template created successfully name=${command.name} channel=${command.channel}")
        }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
        Either.catch { NotificationType.valueOf(channel.trim().uppercase()) }.mapLeft {
            InvalidPayloadError("Invalid channel: $channel")
        }
}
