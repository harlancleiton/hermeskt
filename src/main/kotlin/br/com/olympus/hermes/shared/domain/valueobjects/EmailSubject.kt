package br.com.olympus.hermes.shared.domain.valueobjects

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.exceptions.InvalidEmailSubjectError

@JvmInline
value class EmailSubject private constructor(val subject: String) {
    companion object {
        private const val MAX_LENGTH = 255

        fun create(subject: String): Either<InvalidEmailSubjectError, EmailSubject> {
            val trimmed = subject.trim()
            return when {
                trimmed.isEmpty() ->
                        Either.Left(InvalidEmailSubjectError("Subject cannot be empty"))
                trimmed.length > MAX_LENGTH ->
                        Either.Left(
                                InvalidEmailSubjectError("Subject cannot exceed 255 characters")
                        )
                else -> Either.Right(EmailSubject(trimmed))
            }
        }
    }
}
