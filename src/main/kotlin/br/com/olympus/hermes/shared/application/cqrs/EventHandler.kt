package br.com.olympus.hermes.shared.application.cqrs

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError

/**
 * Base interface for all event handlers (projectors) in the CQRS pattern. An event handler
 * listens to domain events arriving from Kafka and projects them into the read model (MongoDB).
 * Implementations must be **idempotent** — re-processing the same event must produce the same
 * state.
 *
 * @param E The domain event type this handler processes.
 */
interface EventHandler<in E : DomainEvent> {
    /**
     * Handles the given domain event and updates the read model accordingly.
     *
     * @param event The domain event to project.
     * @return Either a [BaseError] on failure or [Unit] on success.
     */
    fun handle(event: E): Either<BaseError, Unit>
}
