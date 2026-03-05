package br.com.olympus.hermes.core.application.commands

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.CommandHandler
import br.com.olympus.hermes.shared.domain.entities.NotificationTemplate
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPayloadError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateDuplicateError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.repositories.TemplateRepository
import br.com.olympus.hermes.shared.domain.valueobjects.TemplateBody
import br.com.olympus.hermes.shared.domain.valueobjects.TemplateName
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import java.util.Date

@ApplicationScoped
class CreateTemplateHandler(
    private val templateRepository: TemplateRepository,
) : CommandHandler<CreateTemplateCommand> {
    @WithSpan("template.create")
    override fun handle(command: CreateTemplateCommand): Either<BaseError, Unit> =
        either {
            Span.current().apply {
                setAttribute("template.name.raw", command.name)
                setAttribute("template.channel.raw", command.channel)
                setAttribute("template.subject.present", !command.subject.isNullOrBlank())
            }
            Log.info(
                "Creating template name=${command.name} channel=${command.channel}",
            )
            val name = TemplateName.create(command.name).bind()
            val channel = parseChannel(command.channel).bind()
            val body = TemplateBody.create(command.body).bind()

            val exists = templateRepository.existsByNameAndChannel(name, channel).bind()
            if (exists) {
                raise(TemplateDuplicateError(name.value, channel.name))
            }

            val now = Date()
            val template =
                NotificationTemplate(
                    name = name,
                    channel = channel,
                    subject = command.subject,
                    body = body,
                    description = command.description,
                    createdAt = now,
                    updatedAt = now,
                )

            templateRepository.save(template).bind()
            Span.current().apply {
                setAttribute("template.created", true)
                setAttribute("template.channel", channel.name)
                setAttribute("template.name", name.value)
            }
            Log.info(
                "Template persisted name=${name.value} channel=${channel.name}",
            )
            Unit
        }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
        Either.catch { NotificationType.valueOf(channel.trim().uppercase()) }.mapLeft {
            InvalidPayloadError("Invalid channel: $channel")
        }
}
