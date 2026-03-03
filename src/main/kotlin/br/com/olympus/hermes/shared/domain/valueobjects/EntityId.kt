package br.com.olympus.hermes.shared.domain.valueobjects

import arrow.core.Either
import arrow.core.Either.Companion.catch
import br.com.olympus.hermes.shared.domain.exceptions.InvalidUUIDError
import java.util.UUID

@JvmInline
value class EntityId private constructor(
    val value: UUID,
) {
    companion object {
        fun from(value: UUID): EntityId = EntityId(value)

        fun from(value: String): Either<InvalidUUIDError, EntityId> =
            catch { UUID.fromString(value) }.map { EntityId(it) }.mapLeft {
                InvalidUUIDError(value, it)
            }

        fun generate(): EntityId {
            val uuid = UUID.randomUUID()
            return EntityId(uuid)
        }
    }
}
