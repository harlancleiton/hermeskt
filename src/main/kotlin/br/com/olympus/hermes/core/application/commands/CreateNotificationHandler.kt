package br.com.olympus.hermes.core.application.commands

import arrow.core.Either
import arrow.core.flatMap
import br.com.olympus.hermes.shared.application.cqrs.CommandHandler
import br.com.olympus.hermes.shared.application.ports.DomainEventPublisher
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.factories.NotificationFactoryRegistry
import br.com.olympus.hermes.shared.domain.repositories.NotificationRepository

class CreateNotificationHandler(
    private val notificationRepository: NotificationRepository,
    private val eventPublisher: DomainEventPublisher,
    private val factoryRegistry: NotificationFactoryRegistry,
) : CommandHandler<CreateNotificationCommand, Notification> {
    override fun handle(command: CreateNotificationCommand): Either<BaseError, Notification> =
        factoryRegistry
            .getFactory<Notification>(command.type)
            .flatMap { factory -> factory.create(command.toInput()) }
            .flatMap { notificationRepository.save(it) }
            .onRight { it.commit() }
}
