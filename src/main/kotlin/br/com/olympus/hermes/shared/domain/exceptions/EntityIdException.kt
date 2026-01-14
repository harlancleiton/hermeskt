package br.com.olympus.hermes.shared.domain.exceptions

sealed class EntityIdException(message: String, cause: Throwable?) : BaseException.ServerException(message, cause) {
    data class InvalidUUID(val value: String, override val cause: Throwable?) :
        EntityIdException("Value '$value' is not a valid UUID", cause)
}
