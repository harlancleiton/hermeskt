package br.com.olympus.hermes.shared.domain.exceptions


sealed interface BaseError {
    val message: String

    val cause: Throwable?

    fun isClientError(): Boolean

    fun isServerError(): Boolean
}

sealed interface ClientError : BaseError {
    override val cause: Throwable? get() = null

    override fun isClientError() = true

    override fun isServerError() = false
}

interface ServerError : BaseError {
    override fun isClientError() = false

    override fun isServerError() = true
}

data class InvalidEmailError(val value: String) : ClientError {
    override val message = "The provided value '$value' is not a valid email."

    override val cause: Throwable?
        get() = null
}

data class InvalidEmailSubjectError(override val message: String) : ClientError {
    override val cause: Throwable?
        get() = null
}

data class InvalidUUIDError(val value: String, override val cause: Throwable?) : ServerError {
    override val message = "The provided value '$value' is not a valid UUID."
}

data class InvalidPhoneError(val value: String) : ClientError {
    override val message = "The provided value '$value' is not a valid phone number."

    override val cause: Throwable?
        get() = null
}