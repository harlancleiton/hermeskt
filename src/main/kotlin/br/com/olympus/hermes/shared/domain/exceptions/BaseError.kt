package br.com.olympus.hermes.shared.domain.exceptions

import br.com.olympus.hermes.notification.domain.factories.NotificationType

/**
 * Sealed interface representing all domain errors in the system. All error types must extend this
 * interface to enable type-safe error handling with ArrowKT.
 */
sealed interface BaseError {
    val message: String

    val cause: Throwable?

    fun isClientError(): Boolean

    fun isServerError(): Boolean
}

/**
 * Sealed interface for client errors (4xx HTTP status codes). These errors indicate issues with the
 * request or input data.
 */
sealed interface ClientError : BaseError {
    override val cause: Throwable?
        get() = null

    override fun isClientError() = true

    override fun isServerError() = false
}

/**
 * Interface for server errors (5xx HTTP status codes). These errors indicate internal server issues
 * or unexpected conditions.
 */
interface ServerError : BaseError {
    override fun isClientError() = false

    override fun isServerError() = true
}

// ========================================
// Validation Errors
// ========================================

/**
 * Error representing accumulated validation failures. Wraps a [List] of [BaseError] produced by
 * ArrowKT's `zipOrAccumulate` during factory validation.
 *
 * @property errors The list of individual validation errors.
 */
data class ValidationErrors(
    val errors: List<BaseError>,
) : ClientError {
    override val message: String = "Validation failed: ${errors.joinToString("; ") { it.message }}"
}

/**
 * Error indicating that the factory received an input subtype it does not support.
 *
 * @property expected The expected input type name.
 * @property actual The actual input type name received.
 */
data class InvalidNotificationInputError(
    val expected: String,
    val actual: String,
) : ClientError {
    override val message: String =
        "Invalid notification input: expected '$expected', got '$actual'."
}

// ========================================
// Value Object Errors
// ========================================

data class InvalidDeviceTokenError(
    val value: String,
) : ClientError {
    override val message = "The provided value '$value' is not a valid device token."

    override val cause: Throwable?
        get() = null
}

data class InvalidEmailError(
    val value: String,
) : ClientError {
    override val message = "The provided value '$value' is not a valid email."

    override val cause: Throwable?
        get() = null
}

data class InvalidEmailSubjectError(
    override val message: String,
) : ClientError {
    override val cause: Throwable?
        get() = null
}

data class InvalidUUIDError(
    val value: String,
    override val cause: Throwable?,
) : ServerError {
    override val message = "The provided value '$value' is not a valid UUID."
}

data class InvalidPhoneError(
    val value: String,
) : ClientError {
    override val message = "The provided value '$value' is not a valid phone number."

    override val cause: Throwable?
        get() = null
}

/**
 * Error indicating that the content provided to the factory is empty or blank.
 *
 * @property field The name of the field that contains empty content.
 */
data class EmptyContentError(
    val field: String,
) : ClientError {
    override val message: String = "The field '$field' cannot be empty."
}

/**
 * Error indicating that the payload provided to the factory is invalid.
 *
 * @property reason The reason why the payload is invalid.
 */
data class InvalidPayloadError(
    val reason: String,
) : ClientError {
    override val message: String = "Invalid payload: $reason"
}

data class InvalidTemplateNameError(
    val value: String,
) : ClientError {
    override val message: String = "The provided template name '$value' is not valid."
}

data class InvalidTemplateBodyError(
    val reason: String,
) : ClientError {
    override val message: String = "The provided template body is not valid: $reason"
}

/**
 * Error indicating that a factory for the requested notification type was not found.
 *
 * @property type The notification type that was requested.
 */
data class InvalidNotificationTypeError(
    val value: String,
) : ClientError {
    override val message: String = "Invalid notification type: $value"
}

data class FactoryNotFoundError(
    val type: NotificationType,
) : ClientError {
    override val message: String = "No factory registered for notification type: $type"
}

/**
 * Error indicating that a factory for the notification type is already registered.
 *
 * @property type The notification type that is already registered.
 */
data class FactoryAlreadyRegisteredError(
    val type: NotificationType,
) : ClientError {
    override val message: String = "Factory for type $type is already registered."
}

/**
 * Error indicating that the event history is empty or invalid during reconstitution.
 *
 * @property reason The reason why the event history is invalid.
 */
data class InvalidEventHistoryError(
    val reason: String,
) : ServerError {
    override val message: String = "Cannot reconstitute entity: $reason"
    override val cause: Throwable? = null
}

/**
 * Error indicating that a required creation event is missing from the event history.
 *
 * @property expectedEventType The type of event that was expected.
 */
data class MissingCreationEventError(
    val expectedEventType: String,
) : ServerError {
    override val message: String = "Event history must contain $expectedEventType"
    override val cause: Throwable? = null
}

data class TemplateNotFoundError(
    val name: String,
    val channel: String,
) : ClientError {
    override val message: String = "Template '$name' for channel '$channel' was not found."
}

data class TemplateDuplicateError(
    val name: String,
    val channel: String,
) : ClientError {
    override val message: String = "Template '$name' for channel '$channel' already exists."
}

data class MissingTemplateVariablesError(
    val missing: List<String>,
) : ClientError {
    override val message: String = "Missing template variables: ${missing.joinToString(", ")}"
}

// ========================================
// Infrastructure Errors
// ========================================

/**
 * Error indicating a failure during notification persistence.
 *
 * @property reason A description of the persistence failure.
 */
data class PersistenceError(
    val reason: String,
    override val cause: Throwable? = null,
) : ServerError {
    override val message: String = "Failed to persist notification: $reason"
}

/**
 * Error indicating a failure during domain event publishing.
 *
 * @property reason A description of the publishing failure.
 */
data class EventPublishingError(
    val reason: String,
    override val cause: Throwable? = null,
) : ServerError {
    override val message: String = "Failed to publish domain event: $reason"
}

/**
 * Error indicating a failure while projecting a domain event into the read model.
 *
 * @property reason A description of the projection failure.
 */
data class ProjectionError(
    val reason: String,
    override val cause: Throwable? = null,
) : ServerError {
    override val message: String = "Failed to project event into read model: $reason"
}

/**
 * Error indicating that the delivery of a notification to the external provider has failed.
 *
 * @property reason A description of the delivery failure.
 */
data class DeliveryError(
    val reason: String,
    override val cause: Throwable? = null,
) : ServerError {
    override val message: String = "Notification delivery failed: $reason"
}

/**
 * Error indicating that no provider adapter is registered for the given notification type.
 *
 * @property type The notification type for which no adapter was found.
 */
data class ProviderAdapterNotFoundError(
    val type: NotificationType,
) : ServerError {
    override val message: String = "No provider adapter registered for notification type: $type"
    override val cause: Throwable? = null
}

// ========================================
// Query / Read Model Errors
// ========================================

/**
 * Error indicating that the requested notification was not found in the read model.
 *
 * @property id The identifier that was searched for.
 */
data class NotificationNotFoundError(
    val id: String,
) : ClientError {
    override val message: String = "Notification with id '$id' was not found."
}
