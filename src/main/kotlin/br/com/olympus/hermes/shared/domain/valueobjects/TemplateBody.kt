package br.com.olympus.hermes.shared.domain.valueobjects

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import br.com.olympus.hermes.shared.domain.exceptions.InvalidTemplateBodyError

@JvmInline
value class TemplateBody
    private constructor(
        val value: String,
    ) {
        companion object {
            private const val MAX_LENGTH = 65536

            fun create(value: String): Either<InvalidTemplateBodyError, TemplateBody> =
                either {
                    val trimmed = value.trim()
                    ensure(trimmed.isNotEmpty()) { InvalidTemplateBodyError("Body cannot be blank") }
                    ensure(trimmed.length <= MAX_LENGTH) {
                        InvalidTemplateBodyError("Body cannot exceed 65536 characters")
                    }
                    TemplateBody(value)
                }
        }
    }
