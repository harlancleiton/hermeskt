package br.com.olympus.hermes.notification.domain.valueobjects

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import br.com.olympus.hermes.shared.domain.exceptions.InvalidDeviceTokenError

@JvmInline
value class DeviceToken private constructor(
    val value: String,
) {
    companion object {
        fun create(value: String): Either<InvalidDeviceTokenError, DeviceToken> =
            either {
                val trimmed = value.trim()
                ensure(trimmed.isNotBlank() && trimmed.length <= 4096) { InvalidDeviceTokenError(value) }
                DeviceToken(trimmed)
            }
    }
}
