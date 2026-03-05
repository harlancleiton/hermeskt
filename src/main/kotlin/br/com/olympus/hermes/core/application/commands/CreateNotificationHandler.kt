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
import br.com.olympus.hermes.shared.domain.services.TemplateEngine
import br.com.olympus.hermes.shared.domain.valueobjects.TemplateName
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class CreateNotificationHandler(
    private val notificationRepository: NotificationRepository,
    private val eventPublisher: DomainEventPublisher,
    private val factoryRegistry: NotificationFactoryRegistry,
    private val templateEngine: TemplateEngine,
) : CommandHandler<CreateNotificationCommand> {
    override fun handle(command: CreateNotificationCommand): Either<BaseError, Unit> =
        either {
            val resolvedCommand = resolveTemplateIfNeeded(command).bind()
            val factory = factoryRegistry.getFactory<Notification>(command.type).bind()
            val notification = factory.create(resolvedCommand.toInput()).bind()
            val saved = notificationRepository.save(notification).bind()
            saved.commit(eventPublisher).bind()
        }

    private fun resolveTemplateIfNeeded(
        command: CreateNotificationCommand,
    ): Either<BaseError, CreateNotificationCommand> =
        either {
            val templateName = command.templateName
            if (templateName.isNullOrBlank()) {
                return@either command
            }

            if (command.type == NotificationType.WHATSAPP) {
                return@either command
            }

            val name = TemplateName.create(templateName).bind()
            val resolved = templateEngine.resolve(name, command.type, command.payload).bind()

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
            }
        }
}
