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
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import java.util.Date

@ApplicationScoped
class UpdateTemplateHandler(
    private val templateRepository: TemplateRepository,
) : CommandHandler<UpdateTemplateCommand> {
    @WithSpan("template.update")
    override fun handle(command: UpdateTemplateCommand): Either<BaseError, Unit> =
        either {
            Span.current().apply {
                setAttribute("template.name.raw", command.name)
                setAttribute("template.channel.raw", command.channel)
                setAttribute("template.subject.updated", command.subject != null)
                setAttribute("template.body.updated", command.body != null)
            }
            Log.info(
                "Updating template name=${command.name} channel=${command.channel}",
            )
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
            Span.current().apply {
                setAttribute("template.updated", true)
                setAttribute("template.channel", channel.name)
                setAttribute("template.name", name.value)
            }
            Log.info(
                "Template updated name=${name.value} channel=${channel.name}",
            )
            Unit
        }

    private fun parseChannel(channel: String): Either<BaseError, NotificationType> =
        Either.catch { NotificationType.valueOf(channel.trim().uppercase()) }.mapLeft {
            InvalidPayloadError("Invalid channel: $channel")
        }
}
