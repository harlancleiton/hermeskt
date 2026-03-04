package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.events.EventWrapper
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.Date

abstract class AggregateRoot
    protected constructor(
        id: EntityId,
        createdAt: Date,
        updatedAt: Date,
    ) : BaseEntity(id, createdAt, updatedAt) {
        var version = 0
            private set

        protected var changes = mutableListOf<EventWrapper>()

        val uncommittedChanges: List<EventWrapper>
            get() = changes.toList()

        protected abstract fun apply(event: DomainEvent)

        fun commit() {
            if (changes.isEmpty()) return

            // TODO Publish events
            changes.clear()
        }

        fun uncommit() {
            version -= changes.size
            changes.clear()
        }

        fun loadFromHistory(history: List<EventWrapper>) {
            history.forEach { envelope ->
                apply(envelope.payload)
                version = envelope.aggregateVersion
            }
        }

        protected fun applyChange(event: DomainEvent) {
            apply(event)
            version++
            updatedAt = Date()
            val envelope =
                EventWrapper(
                    aggregateId = id,
                    aggregateType = this::class.simpleName ?: "Aggregate",
                    aggregateVersion = version,
                    eventType = event::class.simpleName ?: "UnknownEvent",
                    occurredAt = updatedAt,
                    payload = event,
                )
            changes.add(envelope)
        }
    }
