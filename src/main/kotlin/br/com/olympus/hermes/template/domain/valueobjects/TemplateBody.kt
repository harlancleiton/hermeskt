package br.com.olympus.hermes.template.domain.valueobjects

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import br.com.olympus.hermes.shared.domain.exceptions.InvalidTemplateBodyError

/**
 * A value object representing the body of a notification template.
 *
 * @param value The raw string content of the body.
 */
@JvmInline
value class TemplateBody private constructor(
    val value: String,
) {
    companion object {
        private const val MAX_LENGTH = 65536

        /**
         * Creates a [TemplateBody] from the given [value], validating constraints.
         *
         * @param value The raw body string.
         * @return [Either.Right] with the [TemplateBody] if valid, or [Either.Left] with an error
         * if invalid.
         */
        fun create(value: String): Either<InvalidTemplateBodyError, TemplateBody> =
            either {
                val trimmed = value.trim()
                ensure(trimmed.isNotEmpty()) { InvalidTemplateBodyError("Body cannot be blank") }
                ensure(trimmed.length <= MAX_LENGTH) {
                    InvalidTemplateBodyError("Body cannot exceed $MAX_LENGTH characters")
                }
                TemplateBody(trimmed)
            }
    }
}
