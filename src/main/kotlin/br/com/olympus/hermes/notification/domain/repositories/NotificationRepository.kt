package br.com.olympus.hermes.notification.domain.repositories

import arrow.core.Either
import br.com.olympus.hermes.notification.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId

/**
 * Port interface for persisting notification aggregates. Implementations should handle the mapping
 * between domain entities and the underlying storage mechanism.
 */
interface NotificationRepository {
    /**
     * Persists a notification aggregate.
     *
     * @param notification The notification to persist.
     * @return Either a BaseError on failure or the persisted notification on success.
     */
    fun save(notification: Notification): Either<BaseError, Notification>

    /**
     * Retrieves a notification by its unique identifier.
     *
     * @param id The entity identifier.
     * @return Either a BaseError on failure or the notification if found (null if not found).
     */
    fun findById(id: EntityId): Either<BaseError, Notification?>
}
