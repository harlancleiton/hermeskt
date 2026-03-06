package br.com.olympus.hermes.shared.infrastructure.rest.exceptions

import br.com.olympus.hermes.shared.domain.exceptions.BaseError

/**
 * Runtime exception wrapping a domain [BaseError].
 *
 * Used exclusively in the infrastructure/REST layer to bridge Arrow-kt's [Either] error channel
 * with JAX-RS exception-based error handling. The paired [DomainExceptionMapper] intercepts this
 * exception and translates the wrapped [BaseError] into the appropriate HTTP response.
 *
 * @property error The domain error that caused this exception.
 */
class DomainException(
    val error: BaseError,
) : RuntimeException(error.message)
