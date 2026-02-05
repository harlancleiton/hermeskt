package br.com.olympus.hermes.shared.domain.exceptions


sealed interface BaseException {
    val message: String

    val cause: Throwable?

    fun isClientError(): Boolean

    fun isServerError(): Boolean
}

sealed interface ClientException : BaseException {
    override val cause: Throwable? get() = null

    override fun isClientError() = true

    override fun isServerError() = false
}

interface ServerException : BaseException {
    override fun isClientError() = false

    override fun isServerError() = true
}

data class InvalidEmail(val value: String) : ClientException {
    override val message = "The provided value '$value' is not a valid email."

    override val cause: Throwable?
        get() = null
}

data class InvalidEmailSubject(override val message: String) : ClientException {
    override val cause: Throwable?
        get() = null
}

data class InvalidUUID(val value: String, override val cause: Throwable?) : ServerException {
    override val message = "The provided value '$value' is not a valid UUID."
}
