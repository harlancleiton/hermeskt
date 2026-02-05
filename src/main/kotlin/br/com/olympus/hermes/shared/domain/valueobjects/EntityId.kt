package br.com.olympus.hermes.shared.domain.valueobjects

import arrow.core.Either
import arrow.core.Either.Companion.catch
import br.com.olympus.hermes.shared.domain.exceptions.InvalidUUID

import java.util.*

@JvmInline
value class EntityId private constructor(val value: UUID) {
    companion object {
        fun from(value: UUID): EntityId {
            return EntityId(value)
        }

        fun from(value: String): Either<InvalidUUID, EntityId> {
            return catch {
                UUID.fromString(value)
            }.map {
                EntityId(it)
            }.mapLeft { InvalidUUID(value, it) }
        }

        fun generate(): EntityId {
            val uuid = UUID.randomUUID()
            return EntityId(uuid)
        }
    }
}
