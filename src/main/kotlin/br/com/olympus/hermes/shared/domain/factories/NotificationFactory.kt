package br.com.olympus.hermes.shared.domain.factories

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.ValidationErrors

/**
 * Factory interface for creating and reconstituting notification entities. Uses functional error
 * handling with ArrowKT's Either and `zipOrAccumulate` to validate all input fields and accumulate
 * errors explicitly.
 *
 * @param T The type of notification this factory creates.
 */
interface NotificationFactory<T : Notification> {
    /**
     * Creates a new notification instance from raw input data. Validates all fields using
     * `zipOrAccumulate` and returns either a non-empty list of accumulated validation errors or a
     * valid notification.
     *
     * @param input The raw input data containing primitive types to be validated.
     * @return Either a [List] of [BaseError] on validation failure, or the created notification on
     * success.
     */
    fun create(input: CreateNotificationInput): Either<ValidationErrors, T>

    /**
     * Reconstitutes a notification entity from its event history (Event Sourcing pattern).
     * Validates that the event history is valid and contains all required events.
     *
     * @param events The list of domain events representing the entity's history. Must not be empty.
     * @return Either a BaseError (Left) or the reconstituted notification (Right).
     */
    fun reconstitute(events: List<DomainEvent>): Either<BaseError, T>
}
