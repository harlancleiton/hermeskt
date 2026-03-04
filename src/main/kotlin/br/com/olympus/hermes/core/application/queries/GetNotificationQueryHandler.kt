package br.com.olympus.hermes.core.application.queries

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.QueryHandler
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidUUIDError
import br.com.olympus.hermes.shared.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Query handler that retrieves a single [NotificationView] from MongoDB by its aggregate
 * identifier. Reads exclusively from the read model — never touches DynamoDB.
 */
@ApplicationScoped
class GetNotificationQueryHandler : QueryHandler<GetNotificationQuery, NotificationView?> {
    @Inject
    lateinit var viewRepository: NotificationViewRepository

    /**
     * Handles the [GetNotificationQuery] by looking up the notification view in MongoDB.
     *
     * @param query The query carrying the aggregate identifier.
     * @return Either a [BaseError] on failure, or the [NotificationView] if found (null if not
     * found).
     */
    override fun handle(query: GetNotificationQuery): Either<BaseError, NotificationView?> =
        either {
            val id = EntityId.from(query.id).mapLeft { InvalidUUIDError(query.id, null) }.bind()
            viewRepository.findById(id).bind()
        }
}
