package br.com.olympus.hermes.shared.domain.valueobjects

import br.com.olympus.hermes.shared.domain.exceptions.EntityIdException
import java.util.*

@JvmInline
value class EntityId private constructor(val value: UUID) {
    companion object {
        fun from(value: UUID): EntityId {
            return EntityId(value)
        }

        fun from(value: String): Result<EntityId> {
            return runCatching { EntityId(UUID.fromString(value)) }
                .recoverCatching { throw EntityIdException.InvalidUUID(value, it) }
        }

        fun generate(): EntityId {
            val uuid = java.util.UUID.randomUUID()
            return EntityId(uuid)
        }
    }
}
