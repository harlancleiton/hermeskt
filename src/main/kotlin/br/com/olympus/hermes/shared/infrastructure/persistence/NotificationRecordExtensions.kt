package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.raise.Raise
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*

private val MAP_TYPE_REF = object : TypeReference<Map<String, Any>>() {}

/**
 * Writes common [Notification] fields (shared by all notification types) into a
 * [NotificationRecord]. Type-specific fields must be written separately by the converter.
 */
internal fun NotificationRecord.writeCommonFields(
        notification: Notification,
        type: String,
        objectMapper: ObjectMapper
) {
    this.pk = notification.id.value.toString()
    this.sk = type
    this.type = type
    this.content = notification.content
    this.payload = objectMapper.writeValueAsString(notification.payload)
    this.shippingReceipt = notification.shippingReceipt?.let { objectMapper.writeValueAsString(it) }
    this.sentAt = notification.sentAt?.time
    this.deliveryAt = notification.deliveryAt?.time
    this.seenAt = notification.seenAt?.time
    this.createdAt = notification.createdAt.time
    this.updatedAt = notification.updatedAt.time
    this.version = notification.version
}

/** Reads the `id` field and converts it to an [EntityId] inside a [Raise] context. */
internal fun Raise<BaseError>.readEntityId(record: NotificationRecord): EntityId =
        EntityId.from(record.id).bind()

/** Reads a required nullable field, raising a [PersistenceError] if it is null. */
internal fun <T> Raise<BaseError>.requireField(value: T?, fieldName: String, type: String): T =
        value ?: raise(PersistenceError("Missing $fieldName for $type record"))

/** Deserializes the [NotificationRecord.payload] JSON string into a [Map]. */
internal fun NotificationRecord.deserializePayload(objectMapper: ObjectMapper): Map<String, Any> =
        objectMapper.readValue(payload, MAP_TYPE_REF)

/** Deserializes the [NotificationRecord.shippingReceipt] JSON string, or returns null. */
internal fun NotificationRecord.deserializeShippingReceipt(objectMapper: ObjectMapper): Any? =
        shippingReceipt?.let { objectMapper.readValue(it, Any::class.java) }

/** Converts epoch-millis timestamps from the record into [Date] instances. */
internal fun NotificationRecord.readDates(): NotificationDates =
        NotificationDates(
                sentAt = sentAt?.let { Date(it) },
                deliveryAt = deliveryAt?.let { Date(it) },
                seenAt = seenAt?.let { Date(it) },
                createdAt = Date(createdAt),
                updatedAt = Date(updatedAt)
        )

/** Holds the common date fields parsed from a [NotificationRecord]. */
internal data class NotificationDates(
        val sentAt: Date?,
        val deliveryAt: Date?,
        val seenAt: Date?,
        val createdAt: Date,
        val updatedAt: Date
)
