package br.com.olympus.hermes.shared.domain.factories

import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

interface NotificationFactory<T : Notification> {
    fun create(
        content: String,
        payload: Map<String, Any> = emptyMap(),
        id: EntityId? = null,
        createdAt: Date? = null
    ): T

    fun reconstitute(events: List<DomainEvent>): T
}

