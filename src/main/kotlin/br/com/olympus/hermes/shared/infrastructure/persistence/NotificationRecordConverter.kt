package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass

/**
 * Strategy interface for bidirectional conversion between [Notification] domain entities and
 * [NotificationRecord] DynamoDB items. Each notification type (Email, SMS, Push, etc.) provides its
 * own converter implementation, enabling the repository to remain type-agnostic.
 *
 * @param T The concrete notification entity type this converter handles.
 */
interface NotificationRecordConverter<T : Notification> {
    /** The DynamoDB discriminator value stored in [NotificationRecord.type]. */
    val type: String

    /** The concrete [KClass] of the notification entity this converter handles. */
    val entityClass: KClass<T>

    /**
     * Populates a [NotificationRecord] from a domain entity. Common fields (id, content, payload,
     * timestamps, version) and type-specific fields are both written by the converter.
     *
     * @param notification The domain entity to convert.
     * @param record The record to populate (pre-allocated by the caller).
     * @param objectMapper Jackson mapper for serializing complex fields.
     */
    fun populateRecord(
        notification: T,
        record: NotificationRecord,
        objectMapper: ObjectMapper,
    )

    /**
     * Reconstructs a domain entity from a [NotificationRecord]. Validates required fields and
     * recreates value objects, returning [BaseError] on data integrity issues.
     *
     * @param record The DynamoDB record to convert.
     * @param objectMapper Jackson mapper for deserializing complex fields.
     * @return Either a [BaseError] or the reconstructed domain entity.
     */
    fun fromRecord(
        record: NotificationRecord,
        objectMapper: ObjectMapper,
    ): Either<BaseError, T>
}
