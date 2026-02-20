package br.com.olympus.hermes.shared.domain.exceptions

import br.com.olympus.hermes.shared.domain.factories.NotificationType

/**
 * Sealed interface representing all domain errors in the system.
 * All error types must extend this interface to enable type-safe error handling with ArrowKT.
 */
sealed interface BaseError {
    val message: String

    val cause: Throwable?

    fun isClientError(): Boolean

    fun isServerError(): Boolean
}

/**
 * Sealed interface for client errors (4xx HTTP status codes).
 * These errors indicate issues with the request or input data.
 */
sealed interface ClientError : BaseError {
    override val cause: Throwable? get() = null

    override fun isClientError() = true

    override fun isServerError() = false
}

/**
 * Interface for server errors (5xx HTTP status codes).
 * These errors indicate internal server issues or unexpected conditions.
 */
interface ServerError : BaseError {
    override fun isClientError() = false

    override fun isServerError() = true
}

// ========================================
// Value Object Errors
// ========================================

data class InvalidEmailError(val value: String) : ClientError {
    override val message = "The provided value '$value' is not a valid email."

    override val cause: Throwable?
        get() = null
}

data class InvalidEmailSubjectError(override val message: String) : ClientError {
    override val cause: Throwable?
        get() = null
}

data class InvalidUUIDError(val value: String, override val cause: Throwable?) : ServerError {
    override val message = "The provided value '$value' is not a valid UUID."
}

data class InvalidPhoneError(val value: String) : ClientError {
    override val message = "The provided value '$value' is not a valid phone number."

    override val cause: Throwable?
        get() = null
}

/**
 * Error indicating that the content provided to the factory is empty or blank.
 *
 * @property field The name of the field that contains empty content.
 */
data class EmptyContentError(val field: String) : ClientError {
    override val message: String = "The field '$field' cannot be empty."
}

/**
 * Error indicating that the payload provided to the factory is invalid.
 *
 * @property reason The reason why the payload is invalid.
 */
data class InvalidPayloadError(val reason: String) : ClientError {
    override val message: String = "Invalid payload: $reason"
}

/**
 * Error indicating that a factory for the requested notification type was not found.
 *
 * @property type The notification type that was requested.
 */
data class FactoryNotFoundError(val type: NotificationType) : ClientError {
    override val message: String = "No factory registered for notification type: $type"
}

/**
 * Error indicating that a factory for the notification type is already registered.
 *
 * @property type The notification type that is already registered.
 */
data class FactoryAlreadyRegisteredError(val type: NotificationType) : ClientError {
    override val message: String = "Factory for type $type is already registered."
}

/**
 * Error indicating that the event history is empty or invalid during reconstitution.
 *
 * @property reason The reason why the event history is invalid.
 */
data class InvalidEventHistoryError(val reason: String) : ServerError {
    override val message: String = "Cannot reconstitute entity: $reason"
    override val cause: Throwable? = null
}

/**
 * Error indicating that a required creation event is missing from the event history.
 *
 * @property expectedEventType The type of event that was expected.
 */
data class MissingCreationEventError(val expectedEventType: String) : ServerError {
    override val message: String = "Event history must contain $expectedEventType"
    override val cause: Throwable? = null
}
