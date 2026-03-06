package br.com.olympus.hermes.core.application.commands

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.CommandHandler
import br.com.olympus.hermes.shared.application.ports.DomainEventPublisher
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.factories.NotificationFactoryRegistry
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.repositories.NotificationRepository
import br.com.olympus.hermes.template.domain.services.TemplateEngine
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class CreateNotificationHandler(
        private val notificationRepository: NotificationRepository,
        private val eventPublisher: DomainEventPublisher,
        private val factoryRegistry: NotificationFactoryRegistry,
        private val templateEngine: TemplateEngine,
) : CommandHandler<CreateNotificationCommand> {
    @WithSpan("notification.create")
    override fun handle(command: CreateNotificationCommand): Either<BaseError, Unit> = either {
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
    ): Either<BaseError, CreateNotificationCommand> = either {
        val templateName = command.templateName
        if (templateName.isNullOrBlank()) {
            Span.current().setAttribute("notification.template.used", false)
            return@either command
        }

        if (command.type == NotificationType.WHATSAPP) {
            Span.current().setAttribute("notification.template.used", false)
            return@either command
        }

        val name = TemplateName.create(templateName).bind()
        val resolved = templateEngine.resolve(name, command.type, command.payload).bind()
        Span.current().apply {
            setAttribute("notification.template.used", true)
            setAttribute("notification.template.name", name.value)
        }
        Log.info(
                "Resolved template name=${name.value} for notification id=${command.id}",
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
            is CreateNotificationCommand.WhatsApp -> command
            is CreateNotificationCommand.Push -> command
        }
    }
}
