package br.com.olympus.hermes.shared.domain.events

import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.Date

/**
 * Infrastructure envelope wrapping a [DomainEvent] payload with the metadata required for event
 * sourcing persistence, optimistic concurrency control, and message routing.
 *
 * @property eventId Unique identifier for this specific event instance.
 * @property aggregateId The identifier of the aggregate this event belongs to.
 * @property aggregateType The aggregate class name (e.g. "EmailNotification").
 * @property aggregateVersion The aggregate version after this event was applied.
 * @property eventType The concrete [DomainEvent] subtype name (e.g.
 * "EmailNotificationCreatedEvent").
 * @property occurredAt The instant this event was created.
 * @property payload The business-domain event payload.
 */
data class EventWrapper(
    val eventId: EntityId = EntityId.generate(),
    val aggregateId: EntityId,
    val aggregateType: String,
    val aggregateVersion: Int,
    val eventType: String,
    val occurredAt: Date,
    val payload: DomainEvent,
)
