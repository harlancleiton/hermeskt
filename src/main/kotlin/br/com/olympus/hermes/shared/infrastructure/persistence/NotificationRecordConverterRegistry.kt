package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import jakarta.enterprise.context.ApplicationScoped

/**
 * Registry for [NotificationRecordConverter] instances. Provides type-safe lookup by DynamoDB
 * discriminator string (for reads) and by entity runtime class (for writes). Follows the same
 * registry pattern as [br.com.olympus.hermes.shared.domain.factories.NotificationFactoryRegistry].
 */
@ApplicationScoped
class NotificationRecordConverterRegistry {
    private val converters =
            listOf(EmailNotificationRecordConverter(), SmsNotificationRecordConverter())

    private val byType = converters.associateBy { it.type }

    private val byClass = converters.associateBy { it.entityClass }

    /**
     * Retrieves a converter by the DynamoDB `type` discriminator value.
     *
     * @param type The discriminator string stored in [NotificationRecord.type].
     * @return Either a [PersistenceError] if no converter is registered, or the converter.
     */
    fun forType(type: String): Either<BaseError, NotificationRecordConverter<*>> =
            byType[type]?.right()
                    ?: PersistenceError("No converter registered for type: $type").left()

    /**
     * Retrieves the converter that handles the given notification entity's runtime class.
     *
     * @param notification The domain entity whose converter is needed.
     * @return Either a [PersistenceError] if no converter is registered, or the type-safe
     * converter.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Notification> forEntity(
            notification: T
    ): Either<BaseError, NotificationRecordConverter<T>> =
            (byClass[notification::class] as? NotificationRecordConverter<T>)?.right()
                    ?: PersistenceError(
                                    "No converter registered for class: ${notification::class.simpleName}"
                            )
                            .left()
}
