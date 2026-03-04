package br.com.olympus.hermes.shared.application.ports

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.events.EventWrapper
import br.com.olympus.hermes.shared.domain.exceptions.BaseError

/**
 * Port interface for publishing domain events to an external messaging system (e.g., Kafka).
 * Implementations handle the serialization and delivery of events to the appropriate topics.
 */
interface DomainEventPublisher {
    /**
     * Publishes a single domain event.
     *
     * @param event The domain event to publish.
     * @return Either a BaseError on failure or Unit on success.
     */
    fun publish(event: EventWrapper): Either<BaseError, Unit>

    /**
     * Publishes a batch of domain events in order.
     *
     * @param events The list of event wrappers to publish.
     * @return Either a BaseError on failure or Unit on success.
     */
    fun publishAll(events: List<EventWrapper>): Either<BaseError, Unit>
}
