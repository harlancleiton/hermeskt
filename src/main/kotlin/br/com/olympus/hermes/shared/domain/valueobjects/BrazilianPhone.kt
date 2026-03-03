package br.com.olympus.hermes.shared.domain.valueobjects

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPhoneError

data class BrazilianPhone private constructor(
    val value: String,
) {
    val ddd: String
        get() = value.substring(0, 2)

    val number: String
        get() = value.substring(2)

    val masked: String
        get() = "($ddd)$number"

    companion object {
        private val PHONE_REGEX = Regex("^\\d{11}$")

        fun create(value: String): Either<InvalidPhoneError, BrazilianPhone> {
            val trimmed = value.trim()
            return when {
                trimmed.isEmpty() -> Either.Left(InvalidPhoneError(value))
                !PHONE_REGEX.matches(trimmed) -> Either.Left(InvalidPhoneError(value))
                else -> Either.Right(BrazilianPhone(trimmed))
            }
        }
    }
}
