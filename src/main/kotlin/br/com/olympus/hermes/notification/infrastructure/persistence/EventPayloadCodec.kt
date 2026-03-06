package br.com.olympus.hermes.notification.infrastructure.persistence

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import br.com.olympus.hermes.notification.domain.events.*
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Codec responsible for serializing and deserializing the payload of a specific [DomainEvent]
 * subtype [E].
 *
 * Implementations live alongside their event type and are registered in [EventPayloadCodecRegistry]
 * so that [DomainEventSerde] never needs to be modified when new event types are added.
 *
 * @param E The concrete [DomainEvent] subtype this codec handles.
 */
interface EventPayloadCodec<E : DomainEvent> {
    /**
     * The event type discriminator this codec handles (e.g. "EmailNotificationCreatedEvent"). Must
     * match the simple class name of the corresponding [DomainEvent] subtype.
     */
    val eventType: String

    /**
     * Serializes the event-specific fields of [event] into a [Map] that will be written as JSON.
     * Envelope fields (aggregateId, version, occurredAt, etc.) are excluded — they are stored
     * separately in the [EventRecord] columns.
     *
     * @param event The domain event to serialize.
     * @return A map of field name to primitive-friendly value.
     */
    fun serialize(event: E): Map<String, Any?>

    /**
     * Reconstructs an [E] from the [data] map that was previously produced by [serialize]. Envelope
     * fields (aggregateId, version, occurredAt, etc.) are handled by [DomainEventSerde].
     *
     * @param data Deserialized JSON payload map.
     * @param aggregateId The aggregate identifier from the event envelope, for events that carry
     * it.
     * @return Either a [BaseError] if reconstruction fails, or the restored event.
     */
    fun deserialize(
        data: Map<String, Any>,
        aggregateId: String = "",
    ): Either<BaseError, E>
}

/**
 * Registry that maps event type discriminators to their [EventPayloadCodec].
 *
 * Call [register] once per codec (typically at application startup or in a companion object init
 * block) and then use [codecFor] inside [DomainEventSerde] to resolve the right codec at runtime.
 * Adding a new [DomainEvent] only requires registering a new codec here — [DomainEventSerde] itself
 * never changes.
 */
class EventPayloadCodecRegistry {
    private val codecs: MutableMap<String, EventPayloadCodec<*>> = mutableMapOf()

    /**
     * Registers [codec] under its [EventPayloadCodec.eventType] key.
     *
     * @param codec The codec to register.
     */
    fun <E : DomainEvent> register(codec: EventPayloadCodec<E>) {
        codecs[codec.eventType] = codec
    }

    /**
     * Returns the codec registered for [eventType], or `null` if none has been registered.
     *
     * @param eventType The event type discriminator string.
     */
    @Suppress("UNCHECKED_CAST")
    fun <E : DomainEvent> codecFor(eventType: String): EventPayloadCodec<E>? =
        codecs[eventType] as? EventPayloadCodec<E>

    /**
     * Serializes [event] using the codec registered for [event]'s class simple name and then writes
     * the result to JSON via [objectMapper].
     *
     * @param event The domain event to serialize.
     * @param objectMapper Jackson mapper used to write the payload map as a JSON string.
     * @return Either a [PersistenceError] if no codec is registered or JSON writing fails, or the
     * JSON string of the event-specific payload.
     */
    fun serialize(
        event: DomainEvent,
        objectMapper: ObjectMapper,
    ): Either<BaseError, String> {
        val eventType = event::class.simpleName ?: ""

        @Suppress("UNCHECKED_CAST")
        val codec =
            codecs[eventType] as? EventPayloadCodec<DomainEvent>
                ?: return PersistenceError(
                    "No codec registered for event type: $eventType",
                ).left()
        return Either.catch { objectMapper.writeValueAsString(codec.serialize(event)) }.mapLeft {
            PersistenceError("Failed to serialize event: $eventType", it)
        }
    }

    /**
     * Deserializes [json] into a [DomainEvent] using the codec registered for [eventType], passing
     * [aggregateId] through to event constructors that require it.
     *
     * @param eventType The discriminator string.
     * @param aggregateId The aggregate identifier from the event envelope.
     * @param json The JSON-serialized event-specific payload.
     * @param objectMapper Jackson mapper used to read the JSON string.
     * @return Either a [BaseError] on failure, or the reconstructed event.
     */
    @Suppress("UNCHECKED_CAST")
    fun deserialize(
        eventType: String,
        aggregateId: String,
        json: String,
        objectMapper: ObjectMapper,
    ): Either<BaseError, DomainEvent> {
        val codec =
            codecs[eventType] as? EventPayloadCodec<DomainEvent>
                ?: return PersistenceError("No codec registered for event type: $eventType")
                    .left()
        return Either
            .catch {
                objectMapper.readValue(json, object : TypeReference<Map<String, Any>>() {})
            }.mapLeft { PersistenceError("Failed to parse JSON for event: $eventType", it) }
            .flatMap { data -> codec.deserialize(data, aggregateId) }
    }
}
