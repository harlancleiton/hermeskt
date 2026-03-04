package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.domain.events.EventWrapper
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Date

private val MAP_TYPE_REF = object : TypeReference<Map<String, Any>>() {}

/**
 * Serializes and deserializes [br.com.olympus.hermes.shared.domain.events.DomainEvent] payloads
 * to/from JSON for storage in [EventRecord].
 *
 * Dispatches to the [EventPayloadCodec] registered in [registry] for each event type. Adding a new
 * event type only requires registering a new codec in [EventPayloadCodecRegistry] — this class
 * never needs to change.
 *
 * @param objectMapper Jackson mapper used for JSON encoding/decoding.
 * @param registry Registry of [EventPayloadCodec] implementations keyed by event type.
 */
class DomainEventSerde(
    private val objectMapper: ObjectMapper,
    private val registry: EventPayloadCodecRegistry = defaultEventPayloadCodecRegistry(),
) {
    /**
     * Serializes the event-specific payload (excluding envelope fields) into a JSON string.
     *
     * @param event The domain event payload to serialize.
     * @return Either a [PersistenceError] if no codec is registered or serialization fails, or the
     * JSON string of the event-specific payload.
     */
    fun serialize(event: br.com.olympus.hermes.shared.domain.events.DomainEvent): Either<BaseError, String> =
        registry.serialize(event, objectMapper)

    /**
     * Reconstructs an [EventWrapper] from its persisted record fields and JSON payload data.
     *
     * @param eventType The discriminator string (e.g. "EmailNotificationCreatedEvent").
     * @param eventId The unique event identifier.
     * @param aggregateId The aggregate this event belongs to.
     * @param aggregateType The aggregate class name.
     * @param version The aggregate version of this event.
     * @param occurredAt When the event occurred.
     * @param json The JSON-serialized event-specific payload.
     * @return Either a [PersistenceError] on unknown type or data issues, or the reconstructed
     * envelope.
     */
    fun deserialize(
        eventType: String,
        eventId: EntityId,
        aggregateId: EntityId,
        aggregateType: String,
        version: Int,
        occurredAt: Date,
        json: String,
    ): Either<BaseError, EventWrapper> =
        either {
            val codec =
                registry.codecFor<br.com.olympus.hermes.shared.domain.events.DomainEvent>(eventType)
                    ?: raise(PersistenceError("Unknown event type: $eventType"))
            val data: Map<String, Any> = objectMapper.readValue(json, MAP_TYPE_REF)
            val payload = codec.deserialize(data).bind()
            EventWrapper(
                eventId = eventId,
                aggregateId = aggregateId,
                aggregateType = aggregateType,
                aggregateVersion = version,
                eventType = eventType,
                occurredAt = occurredAt,
                payload = payload,
            )
        }
}
