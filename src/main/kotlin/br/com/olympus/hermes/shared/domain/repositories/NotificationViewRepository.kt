package br.com.olympus.hermes.shared.domain.repositories

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId

/**
 * Port interface for reading and writing [br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView]
 * documents in the read model (MongoDB). Implementations live in `infrastructure/readmodel/`.
 * This repository is exclusively used by event handlers (projectors) for writes and query
 * handlers for reads.
 */
interface NotificationViewRepository {
    /**
     * Finds a notification view by its aggregate identifier.
     *
     * @param id The entity identifier.
     * @return Either a [BaseError] on failure or the view document if found (null if not found).
     */
    fun findById(
        id: EntityId,
    ): Either<BaseError, br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView?>

    /**
     * Upserts a notification view document. Creates it if it does not exist, updates it otherwise.
     *
     * @param view The view document to persist.
     * @return Either a [BaseError] on failure or [Unit] on success.
     */
    fun upsert(view: br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView): Either<BaseError, Unit>
}
