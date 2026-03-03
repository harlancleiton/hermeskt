package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import br.com.olympus.hermes.shared.domain.events.*
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*

private val MAP_TYPE_REF = object : TypeReference<Map<String, Any>>() {}

/**
 * Serializes and deserializes [DomainEvent] instances to/from JSON for storage in [EventRecord].
 * Each event type has explicit mapping logic so that value objects are stored as primitives and
 * reconstructed on read.
 */
class DomainEventSerde(private val objectMapper: ObjectMapper) {

    // ========================================
    // Serialize: DomainEvent → JSON string
    // ========================================

    /**
     * Serializes the event-specific payload (excluding common envelope fields) into a JSON string.
     */
    fun serialize(event: DomainEvent): String {
        val data: Map<String, Any?> =
                when (event) {
                    is EmailNotificationCreatedEvent ->
                            mapOf(
                                    "content" to event.content,
                                    "payload" to event.payload,
                                    "from" to event.from.value,
                                    "to" to event.to.value,
                                    "subject" to event.subject.subject,
                            )
                    is SMSNotificationCreatedEvent ->
                            mapOf(
                                    "content" to event.content,
                                    "payload" to event.payload,
                                    "from" to event.from.toInt(),
                                    "to" to event.to.value,
                            )
                    is WhatsAppNotificationCreatedEvent ->
                            mapOf(
                                    "content" to event.content,
                                    "payload" to event.payload,
                                    "from" to event.from.value,
                                    "to" to event.to.value,
                                    "templateName" to event.templateName,
                            )
                    is NotificationSentEvent -> mapOf("shippingReceipt" to event.shippingReceipt)
                    is NotificationSeenEvent -> emptyMap()
                    is NotificationDeliveredEvent -> emptyMap()
                }
        return objectMapper.writeValueAsString(data)
    }

    // ========================================
    // Deserialize: JSON string → DomainEvent
    // ========================================

    /**
     * Reconstructs a [DomainEvent] from its persisted envelope fields and JSON data payload.
     *
     * @param eventType The discriminator string (e.g. "EmailNotificationCreatedEvent").
     * @param eventId The unique event identifier.
     * @param aggregateId The aggregate this event belongs to.
     * @param version The aggregate version of this event.
     * @param occurredAt When the event occurred.
     * @param json The JSON-serialized event-specific data.
     * @return Either a [PersistenceError] on unknown type or data issues, or the reconstructed
     * event.
     */
    fun deserialize(
            eventType: String,
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            json: String
    ): Either<BaseError, DomainEvent> {
        val data: Map<String, Any> = objectMapper.readValue(json, MAP_TYPE_REF)
        return when (eventType) {
            "EmailNotificationCreatedEvent" ->
                    deserializeEmailCreated(eventId, aggregateId, version, occurredAt, data)
            "SMSNotificationCreatedEvent" ->
                    deserializeSmsCreated(eventId, aggregateId, version, occurredAt, data)
            "WhatsAppNotificationCreatedEvent" ->
                    deserializeWhatsAppCreated(eventId, aggregateId, version, occurredAt, data)
            "NotificationSentEvent" ->
                    deserializeNotificationSent(eventId, aggregateId, version, occurredAt, data)
            "NotificationSeenEvent" ->
                    deserializeNotificationSeen(eventId, aggregateId, version, occurredAt)
            "NotificationDeliveredEvent" ->
                    deserializeNotificationDelivered(eventId, aggregateId, version, occurredAt)
            else -> PersistenceError("Unknown event type: $eventType").left()
        }
    }

    // ========================================
    // Per-event deserializers
    // ========================================

    @Suppress("UNCHECKED_CAST")
    private fun deserializeEmailCreated(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            data: Map<String, Any>
    ): Either<BaseError, EmailNotificationCreatedEvent> = either {
        val from = Email.from(data["from"] as String).bind()
        val to = Email.from(data["to"] as String).bind()
        val subject = EmailSubject.create(data["subject"] as String).bind()
        EmailNotificationCreatedEvent(
                id = eventId,
                aggregateId = aggregateId,
                aggregateVersion = version,
                occurredAt = occurredAt,
                content = data["content"] as String,
                payload = (data["payload"] as? Map<String, Any>) ?: emptyMap(),
                from = from,
                to = to,
                subject = subject,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeSmsCreated(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            data: Map<String, Any>
    ): Either<BaseError, SMSNotificationCreatedEvent> = either {
        val to = BrazilianPhone.create(data["to"] as String).bind()
        SMSNotificationCreatedEvent(
                id = eventId,
                aggregateId = aggregateId,
                aggregateVersion = version,
                occurredAt = occurredAt,
                content = data["content"] as String,
                payload = (data["payload"] as? Map<String, Any>) ?: emptyMap(),
                from = (data["from"] as Number).toInt().toUInt(),
                to = to,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeWhatsAppCreated(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            data: Map<String, Any>
    ): Either<BaseError, WhatsAppNotificationCreatedEvent> = either {
        val from = BrazilianPhone.create(data["from"] as String).bind()
        val to = BrazilianPhone.create(data["to"] as String).bind()
        val templateName = data["templateName"] as String
        WhatsAppNotificationCreatedEvent(
                id = eventId,
                aggregateId = aggregateId,
                aggregateVersion = version,
                occurredAt = occurredAt,
                content = data["content"] as String,
                payload = (data["payload"] as? Map<String, Any>) ?: emptyMap(),
                from = from,
                to = to,
                templateName = templateName,
        )
    }

    private fun deserializeNotificationSent(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            data: Map<String, Any>
    ): Either<BaseError, NotificationSentEvent> =
            Either.catch {
                NotificationSentEvent(
                        id = eventId,
                        aggregateId = aggregateId,
                        aggregateVersion = version,
                        occurredAt = occurredAt,
                        shippingReceipt = data["shippingReceipt"] ?: "",
                )
            }
                    .mapLeft { PersistenceError("Failed to deserialize NotificationSentEvent", it) }

    private fun deserializeNotificationSeen(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date
    ): Either<BaseError, NotificationSeenEvent> =
            Either.catch {
                NotificationSeenEvent(
                        id = eventId,
                        aggregateId = aggregateId,
                        aggregateVersion = version,
                        occurredAt = occurredAt,
                )
            }
                    .mapLeft { PersistenceError("Failed to deserialize NotificationSeenEvent", it) }

    private fun deserializeNotificationDelivered(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date
    ): Either<BaseError, NotificationDeliveredEvent> =
            Either.catch {
                NotificationDeliveredEvent(
                        id = eventId,
                        aggregateId = aggregateId,
                        aggregateVersion = version,
                        occurredAt = occurredAt,
                )
            }
                    .mapLeft {
                        PersistenceError("Failed to deserialize NotificationDeliveredEvent", it)
                    }
}
