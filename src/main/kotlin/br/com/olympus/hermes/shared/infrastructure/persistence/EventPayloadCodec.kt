package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Date

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
     * The [DomainEvent.eventType] discriminator this codec handles (e.g.
     * "EmailNotificationCreatedEvent").
     */
    val eventType: String

    /**
     * Serializes the event-specific fields of [event] into a [Map] that will be written as JSON.
     * Common envelope fields (id, aggregateId, version, occurredAt) are excluded — they are stored
     * separately in the [EventRecord] columns.
     *
     * @param event The domain event to serialize.
     * @return A map of field name to primitive-friendly value.
     */
    fun serialize(event: E): Map<String, Any?>

    /**
     * Reconstructs an [E] from the envelope fields and the [data] map that was previously produced
     * by [serialize].
     *
     * @param eventId Restored event id.
     * @param aggregateId Restored aggregate id.
     * @param version Restored aggregate version.
     * @param occurredAt Restored timestamp.
     * @param data Deserialized JSON payload.
     * @return Either a [BaseError] if reconstruction fails, or the restored event.
     */
    fun deserialize(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            data: Map<String, Any>,
    ): Either<BaseError, E>
}

/**
 * Registry that maps [DomainEvent.eventType] discriminators to their [EventPayloadCodec].
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
     * @param eventType The [DomainEvent.eventType] discriminator string.
     */
    @Suppress("UNCHECKED_CAST")
    fun <E : DomainEvent> codecFor(eventType: String): EventPayloadCodec<E>? =
            codecs[eventType] as? EventPayloadCodec<E>

    /**
     * Serializes [event] using the codec registered for [event]'s [DomainEvent.eventType] and then
     * writes the result to JSON via [objectMapper].
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
        @Suppress("UNCHECKED_CAST")
        val codec =
                codecs[event.eventType] as? EventPayloadCodec<DomainEvent>
                        ?: return PersistenceError(
                                        "No codec registered for event type: ${event.eventType}"
                                )
                                .left()
        return Either.catch { objectMapper.writeValueAsString(codec.serialize(event)) }.mapLeft {
            PersistenceError("Failed to serialize event: ${event.eventType}", it)
        }
    }
}
