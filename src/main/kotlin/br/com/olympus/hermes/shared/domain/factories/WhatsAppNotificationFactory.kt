package br.com.olympus.hermes.shared.domain.factories

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import arrow.core.right
import br.com.olympus.hermes.shared.domain.entities.WhatsAppNotification
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.EmptyContentError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidEventHistoryError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidNotificationInputError
import br.com.olympus.hermes.shared.domain.exceptions.MissingCreationEventError
import br.com.olympus.hermes.shared.domain.exceptions.ValidationErrors
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*

/**
 * Stateless factory for creating and reconstituting [WhatsAppNotification] entities. Validates all
 * input fields using ArrowKT's `zipOrAccumulate` to accumulate validation errors instead of failing
 * on the first error.
 */
class WhatsAppNotificationFactory : NotificationFactory<WhatsAppNotification> {

    /**
     * Creates a new [WhatsAppNotification] from raw input data. Validates content, phone number
     * fields, and template name simultaneously, accumulating all validation errors.
     *
     * @param input Must be a [CreateNotificationInput.WhatsApp] instance.
     * @return Either a [ValidationErrors] of accumulated [BaseError]s, or the created
     * [WhatsAppNotification].
     */
    override fun create(
            input: CreateNotificationInput
    ): Either<ValidationErrors, WhatsAppNotification> {
        if (input !is CreateNotificationInput.WhatsApp) {
            return ValidationErrors(
                            listOf(
                                    InvalidNotificationInputError(
                                            expected = "WhatsApp",
                                            actual = input::class.simpleName ?: "Unknown"
                                    )
                            )
                    )
                    .left()
        }

        return either<NonEmptyList<BaseError>, WhatsAppNotification> {
            zipOrAccumulate(
                    { ensure(input.content.isNotBlank()) { EmptyContentError("content") } },
                    { BrazilianPhone.create(input.from).bind() },
                    { BrazilianPhone.create(input.to).bind() },
                    {
                        ensure(input.templateName.isNotBlank()) {
                            EmptyContentError("templateName")
                        }
                    }
            ) { _, from, to, _ ->
                val now = Date()
                WhatsAppNotification(
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
                        templateName = input.templateName,
                        isNew = true
                )
            }
        }
                .mapLeft { ValidationErrors(it) }
    }

    /**
     * Reconstitutes a [WhatsAppNotification] from its event history. Validates that the events list
     * is not empty and contains the required creation event.
     *
     * @param events The domain event history. Must contain at least
     * [WhatsAppNotificationCreatedEvent].
     * @return Either a [BaseError] or the reconstituted [WhatsAppNotification].
     */
    override fun reconstitute(events: List<DomainEvent>): Either<BaseError, WhatsAppNotification> {
        if (events.isEmpty()) {
            return InvalidEventHistoryError("Event history cannot be empty").left()
        }

        val creationEvent =
                events.filterIsInstance<WhatsAppNotificationCreatedEvent>().firstOrNull()
                        ?: return MissingCreationEventError("WhatsAppNotificationCreatedEvent")
                                .left()

        val notification =
                WhatsAppNotification(
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
                        templateName = creationEvent.templateName,
                        isNew = false
                )

        notification.loadFromHistory(events)

        return notification.right()
    }
}
