package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

abstract class BaseEntity protected constructor(
    protected val id: EntityId = EntityId.generate(),
    protected val createdAt: Date = Date(),
    protected var updatedAt: Date = Date()
) {
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is BaseEntity) return false
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id.hashCode()
    }
}
