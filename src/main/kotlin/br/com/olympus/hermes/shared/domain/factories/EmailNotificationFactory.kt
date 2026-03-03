package br.com.olympus.hermes.shared.domain.factories

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import arrow.core.right
import br.com.olympus.hermes.shared.domain.entities.EmailNotification
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.EmptyContentError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidEventHistoryError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidNotificationInputError
import br.com.olympus.hermes.shared.domain.exceptions.MissingCreationEventError
import br.com.olympus.hermes.shared.domain.exceptions.ValidationErrors
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.Date

/**
 * Stateless factory for creating and reconstituting [EmailNotification] entities. Validates all
 * input fields using ArrowKT's `zipOrAccumulate` to accumulate validation errors instead of failing
 * on the first error.
 */
class EmailNotificationFactory : NotificationFactory<EmailNotification> {
    /**
     * Creates a new [EmailNotification] from raw input data. Validates content, from, to, and
     * subject fields simultaneously and accumulates all validation errors.
     *
     * @param input Must be a [CreateNotificationInput.Email] instance.
     * @return Either a [List] of accumulated [BaseError]s, or the created [EmailNotification].
     */
    override fun create(input: CreateNotificationInput): Either<ValidationErrors, EmailNotification> {
        if (input !is CreateNotificationInput.Email) {
            return ValidationErrors(
                listOf(
                    InvalidNotificationInputError(
                        expected = "Email",
                        actual =
                            input::class.simpleName
                                ?: "Unknown",
                    ),
                ),
            ).left()
        }

        return either<NonEmptyList<BaseError>, EmailNotification> {
            zipOrAccumulate(
                {
                    ensure(input.content.isNotBlank()) {
                        EmptyContentError("content")
                    }
                },
                { Email.from(input.from).bind() },
                { Email.from(input.to).bind() },
                { EmailSubject.create(input.subject).bind() },
            ) { _, from, to, subject ->
                val now = Date()
                EmailNotification(
                    content = input.content,
                    payload = input.payload,
                    shippingReceipt = null,
                    sentAt = null,
                    deliveryAt = null,
                    seenAt = null,
                    id = EntityId.generate(),
                    createdAt = now,
                    updatedAt = now,
                    from = from,
                    to = to,
                    subject = subject,
                    isNew = true,
                )
            }
        }.mapLeft { ValidationErrors(it) }
    }

    /**
     * Reconstitutes an [EmailNotification] from its event history. Validates that events list
     * is not empty and contains the required creation event.
     *
     * @param events The domain event history. Must contain at least
     * [EmailNotificationCreatedEvent].
     * @return Either a [BaseError] or the reconstituted [EmailNotification].
     */
    override fun reconstitute(events: List<DomainEvent>): Either<BaseError, EmailNotification> {
        if (events.isEmpty()) {
            return InvalidEventHistoryError("Event history cannot be empty").left()
        }

        val creationEvent =
            events.filterIsInstance<EmailNotificationCreatedEvent>().firstOrNull()
                ?: return MissingCreationEventError("EmailNotificationCreatedEvent")
                    .left()

        val notification =
            EmailNotification(
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
                isNew = false,
            )

        notification.loadFromHistory(events)

        return notification.right()
    }
}
