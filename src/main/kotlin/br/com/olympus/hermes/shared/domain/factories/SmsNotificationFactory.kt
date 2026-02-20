package br.com.olympus.hermes.shared.domain.factories

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import br.com.olympus.hermes.shared.domain.entities.SmsNotification
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.EmptyContentError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidEventHistoryError
import br.com.olympus.hermes.shared.domain.exceptions.MissingCreationEventError
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

/**
 * Factory for creating and reconstituting SmsNotification entities. Implements functional error
 * handling and input validation to ensure data integrity.
 *
 * @property from The sender's phone number (short code).
 * @property to The recipient's phone number.
 */
class SmsNotificationFactory(private val from: UInt, private val to: BrazilianPhone) :
        NotificationFactory<SmsNotification> {

    /**
     * Creates a new SmsNotification with validation. Validates that content is not blank before
     * creating the notification.
     *
     * @param content The SMS content. Must not be blank.
     * @param payload Additional metadata for the notification.
     * @param id Optional entity ID.
     * @param createdAt Optional creation timestamp.
     * @return Either EmptyContentError if content is blank, or the created SmsNotification.
     */
    override fun create(
            content: String,
            payload: Map<String, Any>,
            id: EntityId?,
            createdAt: Date?
    ): Either<BaseError, SmsNotification> {
        // Validate content is not blank (secure coding: input validation)
        if (content.isBlank()) {
            return EmptyContentError("content").left()
        }

        val now = Date()
        val notification =
                SmsNotification(
                        content = content,
                        payload = payload,
                        shippingReceipt = null,
                        sentAt = null,
                        deliveryAt = null,
                        seenAt = null,
                        id = id ?: EntityId.generate(),
                        createdAt = createdAt ?: now,
                        updatedAt = now,
                        from = from,
                        to = to,
                        isNew = true
                )

        return notification.right()
    }

    /**
     * Reconstitutes a SmsNotification from its event history. Validates that events list is not
     * empty and contains the required creation event.
     *
     * @param events The domain event history. Must contain at least SMSNotificationCreatedEvent.
     * @return Either a BaseError or the reconstituted SmsNotification.
     */
    override fun reconstitute(events: List<DomainEvent>): Either<BaseError, SmsNotification> {
        // Validate event history is not empty
        if (events.isEmpty()) {
            return InvalidEventHistoryError("Event history cannot be empty").left()
        }

        // Find and validate the creation event exists
        val creationEvent =
                events.filterIsInstance<SMSNotificationCreatedEvent>().firstOrNull()
                        ?: return MissingCreationEventError("SMSNotificationCreatedEvent").left()

        val notification =
                SmsNotification(
                        content = creationEvent.content,
                        payload = creationEvent.payload,
                        shippingReceipt = null,
                        sentAt = null,
                        deliveryAt = null,
                        seenAt = null,
                        id = creationEvent.aggregateId,
                        createdAt = creationEvent.occurredAt,
                        updatedAt = creationEvent.occurredAt,
                        from = creationEvent.from,
                        to = creationEvent.to,
                        isNew = false
                )

        notification.loadFromHistory(events)

        return notification.right()
    }
}
