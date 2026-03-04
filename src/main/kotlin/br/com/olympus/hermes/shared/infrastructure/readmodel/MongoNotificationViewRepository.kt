package br.com.olympus.hermes.shared.infrastructure.readmodel

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import jakarta.enterprise.context.ApplicationScoped

/**
 * MongoDB implementation of [NotificationViewRepository] using Panache's static helper methods on
 * [NotificationView]. Provides idempotent upsert semantics for projectors.
 */
@ApplicationScoped
class MongoNotificationViewRepository : NotificationViewRepository {
    /**
     * Finds a notification view by its aggregate identifier.
     *
     * @param id The entity identifier.
     * @return Either a [PersistenceError] on failure or the view document if found (null if not
     * found).
     */
    override fun findById(id: EntityId): Either<BaseError, NotificationView?> =
        Either
            .catch { NotificationView.find("_id", id.value.toString()).firstResult() }
            .mapLeft { ex -> PersistenceError(ex.message ?: "Unknown error", ex) }

    /**
     * Upserts a notification view document. Uses Panache `persistOrUpdate` which inserts or
     * replaces by `_id`.
     *
     * @param view The view document to persist.
     * @return Either a [PersistenceError] on failure or [Unit] on success.
     */
    override fun upsert(view: NotificationView): Either<BaseError, Unit> =
        Either
            .catch {
                view.persistOrUpdate()
                Unit
            }.mapLeft { ex -> PersistenceError(ex.message ?: "Unknown error", ex) }
}
