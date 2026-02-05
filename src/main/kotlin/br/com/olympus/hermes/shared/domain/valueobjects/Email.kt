package br.com.olympus.hermes.shared.domain.valueobjects

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import br.com.olympus.hermes.shared.domain.exceptions.InvalidEmailError

@JvmInline
value class Email private constructor(val value: String) {
    companion object {
        fun from(value: String): Either<InvalidEmailError, Email> {
            return either {
                val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
                ensure(emailRegex.matches(value)) { InvalidEmailError(value) }
                Email(value)
            }
        }
    }
}
