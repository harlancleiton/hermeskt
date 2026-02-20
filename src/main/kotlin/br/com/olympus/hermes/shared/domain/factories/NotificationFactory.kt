package br.com.olympus.hermes.shared.domain.factories

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

/**
 * Factory interface for creating and reconstituting notification entities.
 * Uses functional error handling with ArrowKT's Either to handle domain errors explicitly.
 *
 * @param T The type of notification this factory creates.
 */
interface NotificationFactory<T : Notification> {
    /**
     * Creates a new notification instance with the provided data.
     * Validates all input parameters and returns either a validation error or a valid notification.
     *
     * @param content The content of the notification. Must not be blank.
     * @param payload Additional payload data for the notification. Defaults to empty map.
     * @param id Optional entity ID. If not provided, a new ID will be generated.
     * @param createdAt Optional creation timestamp. If not provided, current time will be used.
     * @return Either a BaseError (Left) or the created notification (Right).
     */
    fun create(
        content: String,
        payload: Map<String, Any> = emptyMap(),
        id: EntityId? = null,
        createdAt: Date? = null
    ): Either<BaseError, T>

    /**
     * Reconstitutes a notification entity from its event history (Event Sourcing pattern).
     * Validates that the event history is valid and contains all required events.
     *
     * @param events The list of domain events representing the entity's history. Must not be empty.
     * @return Either a BaseError (Left) or the reconstituted notification (Right).
     */
    fun reconstitute(events: List<DomainEvent>): Either<BaseError, T>
}

