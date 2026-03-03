package br.com.olympus.hermes.shared.domain.repositories

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId

/**
 * Port interface for an append-only event store. Persists domain events as the source of truth for
 * aggregate state (Event Sourcing). Implementations must guarantee ordering by aggregate version
 * and support optimistic concurrency via [expectedVersion].
 */
interface EventStore {
    /**
     * Appends new events for an aggregate. Uses optimistic concurrency control — the operation
     * fails if events already exist at or beyond [expectedVersion].
     *
     * @param aggregateId The aggregate's unique identifier (partition key).
     * @param events The uncommitted domain events to persist, ordered by version.
     * @param expectedVersion The aggregate version before these events were applied. Used to detect
     * concurrent writes.
     * @return Either a [BaseError] on conflict/failure, or [Unit] on success.
     */
    fun append(
        aggregateId: EntityId,
        events: List<DomainEvent>,
        expectedVersion: Int,
    ): Either<BaseError, Unit>

    /**
     * Retrieves the full event stream for an aggregate, ordered by version ascending.
     *
     * @param aggregateId The aggregate's unique identifier.
     * @return Either a [BaseError] on failure, or the ordered list of events (empty if aggregate
     * does not exist).
     */
    fun getEvents(aggregateId: EntityId): Either<BaseError, List<DomainEvent>>
}
