package br.com.olympus.hermes.shared.application.cqrs

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.exceptions.BaseError

/**
 * Base interface for all query handlers in the CQRS pattern. A query handler receives a [Query]
 * and returns the result wrapped in [Either] for functional error handling. Query handlers must
 * read exclusively from the read model (MongoDB views) and never from the write store (DynamoDB).
 *
 * @param Q The query type this handler processes.
 * @param R The result type produced on success.
 */
interface QueryHandler<in Q : Query<R>, R> {
    /**
     * Handles the given query and returns the result.
     *
     * @param query The query to handle.
     * @return Either a [BaseError] on failure or the result [R] on success.
     */
    fun handle(query: Q): Either<BaseError, R>
}
