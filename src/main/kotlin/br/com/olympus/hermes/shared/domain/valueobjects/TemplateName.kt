package br.com.olympus.hermes.shared.domain.valueobjects

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import br.com.olympus.hermes.shared.domain.exceptions.InvalidTemplateNameError

@JvmInline
value class TemplateName
    private constructor(
        val value: String,
    ) {
        companion object {
            private const val MAX_LENGTH = 128
            private val SLUG_REGEX = "^[a-z0-9]+(-[a-z0-9]+)*$".toRegex()

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
