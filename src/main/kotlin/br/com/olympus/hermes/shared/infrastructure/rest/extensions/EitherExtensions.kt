package br.com.olympus.hermes.shared.infrastructure.rest.extensions

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.infrastructure.rest.exceptions.DomainException
import br.com.olympus.hermes.shared.infrastructure.rest.exceptions.DomainExceptionMapper

/**
 * Unwraps the [Either.Right] value or throws a [DomainException] wrapping the [Either.Left] error.
 *
 * Intended for use in JAX-RS controller methods where a paired [DomainExceptionMapper] is
 * registered to translate the exception into the appropriate HTTP error response.
 *
 * @return The right-side value of type [T].
 * @throws DomainException if this is a [Either.Left].
 */
fun <T> Either<BaseError, T>.getOrThrowDomain(): T = fold(ifLeft = { throw DomainException(it) }, ifRight = { it })
