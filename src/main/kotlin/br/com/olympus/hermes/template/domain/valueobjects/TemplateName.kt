package br.com.olympus.hermes.template.domain.valueobjects

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import br.com.olympus.hermes.shared.domain.exceptions.InvalidTemplateNameError

/**
 * A value object representing the unique name of a notification template.
 *
 * Template names must be non-blank slugs (lowercase alphanumeric with hyphens), up to 128
 * characters in length.
 *
 * @param value The raw string name.
 */
@JvmInline
value class TemplateName private constructor(
    val value: String,
) {
    companion object {
        private const val MAX_LENGTH = 128
        private val SLUG_REGEX = "^[a-z0-9]+(-[a-z0-9]+)*$".toRegex()

        /**
         * Creates a [TemplateName] from the given [value], validating constraints.
         *
         * @param value The raw name string.
         * @return [Either.Right] with the [TemplateName] if valid, or [Either.Left] with an error
         * if invalid.
         */
        fun create(value: String): Either<InvalidTemplateNameError, TemplateName> =
            either {
                val trimmed = value.trim()
                ensure(trimmed.isNotEmpty()) { InvalidTemplateNameError(value) }
                ensure(trimmed.length <= MAX_LENGTH) { InvalidTemplateNameError(value) }
                ensure(SLUG_REGEX.matches(trimmed)) { InvalidTemplateNameError(value) }
                TemplateName(trimmed)
            }
    }
}
