package br.com.olympus.hermes.shared.infrastructure.messaging

import br.com.olympus.hermes.shared.domain.events.DomainEvent
import java.util.Date

/**
 * Serializable envelope wrapping a [DomainEvent] for Kafka transport. Carries the event type
 * discriminator and the payload serialized as a generic map so consumers can deserialize correctly.
 *
 * @property eventType The simple class name of the domain event (e.g. "EmailNotificationCreatedEvent").
 * @property occurredAt The timestamp when the event was produced.
 * @property payload The event payload as a generic map for Jackson serialization.
 */
data class KafkaEventEnvelope(
    val eventType: String,
    val occurredAt: Date,
    val payload: DomainEvent,
) {
    companion object {
        /**
         * Creates a [KafkaEventEnvelope] from a [DomainEvent].
         *
         * @param event The domain event to wrap.
         * @return A new [KafkaEventEnvelope] instance.
         */
        fun from(event: DomainEvent): KafkaEventEnvelope =
            KafkaEventEnvelope(
                eventType = event::class.simpleName ?: "UnknownEvent",
                occurredAt = Date(),
                payload = event,
            )
    }
}
