package br.com.olympus.hermes.shared.domain.repositories

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId

/**
 * Port interface for reading and writing
 * [br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView] documents in the read
 * model (MongoDB). Implementations live in `infrastructure/readmodel/`. This repository is
 * exclusively used by event handlers (projectors) for writes and query handlers for reads.
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

    /**
     * Finds all notification views, optionally filtering by status and type, with pagination.
     *
     * @param pageIndex The requested page index (0-based).
     * @param pageSize The number of items per page.
     * @param status Optional filter by notification status.
     * @param type Optional filter by notification type.
     * @return Either a [BaseError] on failure or a [PaginatedResult] on success.
     */
    fun findAll(
        pageIndex: Int,
        pageSize: Int,
        status: String? = null,
        type: String? = null,
    ): Either<
        BaseError,
        PaginatedResult<br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView>,
    >

    /**
     * Counts the total number of notifications.
     *
     * @return Either a [BaseError] on failure or the total count on success.
     */
    fun countAll(): Either<BaseError, Long>

    /**
     * Updates the status and optionally failure reason of a notification view.
     *
     * @param id The entity identifier.
     * @param status The new status.
     * @param failureReason An optional failure reason if the status denotes an error.
     * @return Either a [BaseError] on failure or [Unit] on success.
     */
    fun updateStatus(
        id: EntityId,
        status: String,
        failureReason: String? = null,
    ): Either<BaseError, Unit>
}
