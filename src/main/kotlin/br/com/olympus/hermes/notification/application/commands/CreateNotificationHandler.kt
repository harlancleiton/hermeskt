package br.com.olympus.hermes.notification.application.commands

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.notification.application.ports.TemplateResolver
import br.com.olympus.hermes.notification.domain.entities.Notification
import br.com.olympus.hermes.notification.domain.factories.NotificationFactoryRegistry
import br.com.olympus.hermes.notification.domain.repositories.NotificationRepository
import br.com.olympus.hermes.shared.application.cqrs.CommandHandler
import br.com.olympus.hermes.shared.application.ports.DomainEventPublisher
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class CreateNotificationHandler(
    private val notificationRepository: NotificationRepository,
    private val eventPublisher: DomainEventPublisher,
    private val factoryRegistry: NotificationFactoryRegistry,
    private val templateResolver: TemplateResolver,
) : CommandHandler<CreateNotificationCommand> {
    @WithSpan("notification.create")
    override fun handle(command: CreateNotificationCommand): Either<BaseError, Unit> =
        either {
            Span.current().apply {
                setAttribute("notification.id", command.id)
                setAttribute("notification.type", command.type.name)
                setAttribute("notification.payload.size", command.payload.size.toLong())
                setAttribute(
                    "notification.template.requested",
                    !command.templateName.isNullOrBlank(),
                )
            }
            Log.info(
                "Creating notification id=${command.id} type=${command.type}" +
                    " template=${command.templateName ?: "<none>"}",
            )

            val resolvedCommand = resolveTemplateIfNeeded(command).bind()
            val factory = factoryRegistry.getFactory<Notification>(command.type).bind()
            val notification = factory.create(resolvedCommand.toInput()).bind()
            val saved = notificationRepository.save(notification).bind()
            saved.commit(eventPublisher).bind()

            Span.current().setAttribute("notification.persisted", true)
            Log.info("Notification created id=${command.id} type=${command.type}")
        }

    private fun resolveTemplateIfNeeded(
        command: CreateNotificationCommand,
    ): Either<BaseError, CreateNotificationCommand> =
        either {
            val templateName = command.templateName
            if (templateName.isNullOrBlank()) {
                Span.current().setAttribute("notification.template.used", false)
                return@either command
            }

            if (command.type == NotificationType.WHATSAPP) {
                Span.current().setAttribute("notification.template.used", false)
                return@either command
            }

            val resolved = templateResolver.resolve(templateName, command.type, command.payload).bind()
            Span.current().apply {
                setAttribute("notification.template.used", true)
                setAttribute("notification.template.name", templateName)
            }
            Log.info(
                "Resolved template name=$templateName for notification id=${command.id}",
            )

            when (command) {
                is CreateNotificationCommand.Email ->
                    command.copy(
                        content = resolved.body,
                        subject = resolved.subject ?: command.subject,
                    )
                is CreateNotificationCommand.Sms ->
                    command.copy(
                        content = resolved.body,
                    )
                is CreateNotificationCommand.Push ->
                    command.copy(
                        content = resolved.body,
                        title = resolved.subject ?: command.title,
                    )
                is CreateNotificationCommand.WhatsApp -> command
            }
        }
}
