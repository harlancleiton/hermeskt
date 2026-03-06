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

    override fun findAll(
        pageIndex: Int,
        pageSize: Int,
        status: String?,
        type: String?,
    ): Either<
        BaseError,
        br.com.olympus.hermes.shared.domain.repositories.PaginatedResult<NotificationView>,
    > =
        Either
            .catch {
                val query = mutableListOf<String>()
                val params = mutableMapOf<String, Any>()

                if (status != null) {
                    query.add("status = :status")
                    params["status"] = status
                }
                if (type != null) {
                    query.add("type = :type")
                    params["type"] = type
                }

                val panacheQuery =
                    if (query.isEmpty()) {
                        NotificationView.findAll()
                    } else {
                        NotificationView.find(query.joinToString(" and "), params)
                    }

                val count = panacheQuery.count()
                val items = panacheQuery.page(pageIndex, pageSize).list()

                br.com.olympus.hermes.shared.domain.repositories.PaginatedResult(
                    items = items,
                    totalCount = count,
                    pageIndex = pageIndex,
                    pageSize = pageSize,
                )
            }.mapLeft { ex -> PersistenceError(ex.message ?: "Unknown error", ex) }

    override fun countAll(): Either<BaseError, Long> =
        Either.catch { NotificationView.count() }.mapLeft { ex ->
            PersistenceError(ex.message ?: "Unknown error", ex)
        }

    override fun updateStatus(
        id: EntityId,
        status: String,
        failureReason: String?,
    ): Either<BaseError, Unit> =
        Either
            .catch {
                val view = NotificationView.find("_id", id.value.toString()).firstResult()
                if (view != null) {
                    view.status = status
                    if (failureReason != null) {
                        view.failureReason = failureReason
                    }
                    view.persistOrUpdate()
                }
                Unit
            }.mapLeft { ex -> PersistenceError(ex.message ?: "Unknown error", ex) }
}
