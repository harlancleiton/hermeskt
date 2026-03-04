package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.right
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.events.NotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.factories.NotificationFactoryRegistry
import br.com.olympus.hermes.shared.domain.repositories.EventStore
import br.com.olympus.hermes.shared.domain.repositories.NotificationRepository
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Event-sourced implementation of [NotificationRepository]. Uses the [EventStore] as the source of
 * truth — `save` appends uncommitted domain events, and `findById` replays the event stream through
 * the appropriate [NotificationFactoryRegistry] factory to reconstitute the aggregate.
 */
@ApplicationScoped
class DynamoDbNotificationRepository
    @Inject
    constructor(
        private val eventStore: EventStore,
        private val factoryRegistry: NotificationFactoryRegistry,
    ) : NotificationRepository {
        override fun save(notification: Notification): Either<BaseError, Notification> {
            val events = notification.uncommittedChanges
            if (events.isEmpty()) return notification.right()

            val expectedVersion = notification.version - events.size
            return eventStore.append(notification.id, events, expectedVersion).map { notification }
        }

        override fun findById(id: EntityId): Either<BaseError, Notification?> =
            either {
                val events = eventStore.getEvents(id).bind()
                if (events.isEmpty()) return@either null

                val creationEnvelope = events.find { it.payload is NotificationCreatedEvent }
                val creationEvent = creationEnvelope?.payload as? NotificationCreatedEvent
                if (creationEvent == null) return@either null

                return factoryRegistry.getFactory<Notification>(creationEvent.type).flatMap {
                    it.reconstitute(events)
                }
            }
    }
