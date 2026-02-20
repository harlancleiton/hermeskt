package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

abstract class BaseEntity
protected constructor(
        val id: EntityId = EntityId.generate(),
        val createdAt: Date = Date(),
        updatedAt: Date = Date()
) {
    var updatedAt = updatedAt
        protected set

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is BaseEntity) return false
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id.hashCode()
    }
}
