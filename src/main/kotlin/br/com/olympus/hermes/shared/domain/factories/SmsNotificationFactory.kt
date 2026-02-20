package br.com.olympus.hermes.shared.domain.factories

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import arrow.core.right
import br.com.olympus.hermes.shared.domain.entities.SmsNotification
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.events.SMSNotificationCreatedEvent
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
 * Stateless factory for creating and reconstituting [SmsNotification] entities. Validates all input
 * fields using ArrowKT's `zipOrAccumulate` to accumulate validation errors instead of failing on
 * the first error.
 */
class SmsNotificationFactory : NotificationFactory<SmsNotification> {

        /**
         * Creates a new [SmsNotification] from raw input data. Validates content and phone number
         * fields simultaneously and accumulates all validation errors.
         *
         * @param input Must be a [CreateNotificationInput.Sms] instance.
         * @return Either a [ValidationErrors] of accumulated [BaseError]s, or the created
         * [SmsNotification].
         */
        override fun create(
                input: CreateNotificationInput
        ): Either<ValidationErrors, SmsNotification> {
                if (input !is CreateNotificationInput.Sms) {
                        return ValidationErrors(
                                        listOf(
                                                InvalidNotificationInputError(
                                                        expected = "Sms",
                                                        actual = input::class.simpleName
                                                                        ?: "Unknown"
                                                )
                                        )
                                )
                                .left()
                }

                return either<NonEmptyList<BaseError>, SmsNotification> {
                        zipOrAccumulate(
                                {
                                        ensure(input.content.isNotBlank()) {
                                                EmptyContentError("content")
                                        }
                                },
                                { BrazilianPhone.create(input.to).bind() }
                        ) { _, to ->
                                val now = Date()
                                SmsNotification(
                                        content = input.content,
                                        payload = input.payload,
                                        shippingReceipt = null,
                                        sentAt = null,
                                        deliveryAt = null,
                                        seenAt = null,
                                        id = EntityId.generate(),
                                        createdAt = now,
                                        updatedAt = now,
                                        from = input.from,
                                        to = to,
                                        isNew = true
                                )
                        }
                }
                        .mapLeft { ValidationErrors(it) }
        }

        /**
         * Reconstitutes a [SmsNotification] from its event history. Validates that events list is
         * not empty and contains the required creation event.
         *
         * @param events The domain event history. Must contain at least
         * [SMSNotificationCreatedEvent].
         * @return Either a [BaseError] or the reconstituted [SmsNotification].
         */
        override fun reconstitute(events: List<DomainEvent>): Either<BaseError, SmsNotification> {
                if (events.isEmpty()) {
                        return InvalidEventHistoryError("Event history cannot be empty").left()
                }

                val creationEvent =
                        events.filterIsInstance<SMSNotificationCreatedEvent>().firstOrNull()
                                ?: return MissingCreationEventError("SMSNotificationCreatedEvent")
                                        .left()

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
