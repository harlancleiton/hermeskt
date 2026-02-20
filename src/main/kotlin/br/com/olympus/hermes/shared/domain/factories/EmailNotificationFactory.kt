package br.com.olympus.hermes.shared.domain.factories

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import br.com.olympus.hermes.shared.domain.entities.EmailNotification
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.EmptyContentError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidEventHistoryError
import br.com.olympus.hermes.shared.domain.exceptions.MissingCreationEventError
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

/**
 * Factory for creating and reconstituting EmailNotification entities.
 * Implements functional error handling and input validation to ensure data integrity.
 *
 * @property from The sender's email address.
 * @property to The recipient's email address.
 * @property subject The email subject.
 */
class EmailNotificationFactory(
    private val from: Email,
    private val to: Email,
    private val subject: EmailSubject
) : NotificationFactory<EmailNotification> {

    /**
     * Creates a new EmailNotification with validation.
     * Validates that content is not blank before creating the notification.
     *
     * @param content The email content. Must not be blank.
     * @param payload Additional metadata for the notification.
     * @param id Optional entity ID.
     * @param createdAt Optional creation timestamp.
     * @return Either EmptyContentError if content is blank, or the created EmailNotification.
     */
    override fun create(
        content: String,
        payload: Map<String, Any>,
        id: EntityId?,
        createdAt: Date?
    ): Either<BaseError, EmailNotification> {
        // Validate content is not blank (secure coding: input validation)
        if (content.isBlank()) {
            return EmptyContentError("content").left()
        }

        val now = Date()
        val notification = EmailNotification(
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
            subject = subject,
            isNew = true
        )

        return notification.right()
    }

    /**
     * Reconstitutes an EmailNotification from its event history.
     * Validates that events list is not empty and contains the required creation event.
     *
     * @param events The domain event history. Must contain at least EmailNotificationCreatedEvent.
     * @return Either a BaseError or the reconstituted EmailNotification.
     */
    override fun reconstitute(events: List<DomainEvent>): Either<BaseError, EmailNotification> {
        // Validate event history is not empty
        if (events.isEmpty()) {
            return InvalidEventHistoryError("Event history cannot be empty").left()
        }

        // Find and validate the creation event exists
        val creationEvent = events.filterIsInstance<EmailNotificationCreatedEvent>().firstOrNull()
            ?: return MissingCreationEventError("EmailNotificationCreatedEvent").left()

        val notification = EmailNotification(
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
            subject = creationEvent.subject,
            isNew = false
        )

        notification.loadFromHistory(events)

        return notification.right()
    }
}

