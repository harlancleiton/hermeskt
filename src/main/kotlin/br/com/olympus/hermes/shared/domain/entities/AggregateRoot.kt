package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

abstract class AggregateRoot protected constructor(id: EntityId, createdAt: Date, updatedAt: Date) :
        BaseEntity(id, createdAt, updatedAt) {

    var version = 0

    protected var changes = mutableListOf<DomainEvent>()

    val uncommittedChanges: List<DomainEvent>
        get() = changes.toList()

    abstract fun apply(event: DomainEvent)

    fun commit() {
        if (changes.isEmpty()) {
            return
        }

        version += changes.size
        changes.clear()
    }

    fun uncommit() {
        changes.clear()
    }

    fun loadFromHistory(history: List<DomainEvent>) {
        history.forEach { event ->
            apply(event)
            version = event.aggregateVersion
        }
    }

    protected fun applyChange(event: DomainEvent) {
        apply(event)
        changes.add(event)
        updatedAt = event.occurredAt
    }
}
