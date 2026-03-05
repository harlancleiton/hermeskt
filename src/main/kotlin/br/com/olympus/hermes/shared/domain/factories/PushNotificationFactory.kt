package br.com.olympus.hermes.shared.domain.factories

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import arrow.core.right
import br.com.olympus.hermes.shared.domain.entities.PushNotification
import br.com.olympus.hermes.shared.domain.events.EventWrapper
import br.com.olympus.hermes.shared.domain.events.PushNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.EmptyContentError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidEventHistoryError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidNotificationInputError
import br.com.olympus.hermes.shared.domain.exceptions.MissingCreationEventError
import br.com.olympus.hermes.shared.domain.exceptions.ValidationErrors
import br.com.olympus.hermes.shared.domain.valueobjects.DeviceToken
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.Date

class PushNotificationFactory : NotificationFactory<PushNotification> {
    override fun create(input: CreateNotificationInput): Either<ValidationErrors, PushNotification> {
        if (input !is CreateNotificationInput.Push) {
            return ValidationErrors(
                listOf(
                    InvalidNotificationInputError(
                        expected = "Push",
                        actual = input::class.simpleName ?: "Unknown",
                    ),
                ),
            ).left()
        }

        return either<NonEmptyList<BaseError>, PushNotification> {
            zipOrAccumulate(
                { EntityId.from(input.id).bind() },
                { ensure(input.content.isNotBlank()) { EmptyContentError("content") } },
                { DeviceToken.create(input.deviceToken).bind() },
                { ensure(input.title.isNotBlank()) { EmptyContentError("title") } },
            ) { id, _, token, _ ->
                val now = Date()
                PushNotification(
                    content = input.content,
                    payload = input.payload,
                    shippingReceipt = null,
                    sentAt = null,
                    deliveryAt = null,
                    seenAt = null,
                    id = id,
                    createdAt = now,
                    updatedAt = now,
                    deviceToken = token,
                    title = input.title,
                    data = input.data,
                    isNew = true,
                )
            }
        }.mapLeft { ValidationErrors(it) }
    }

    override fun reconstitute(events: List<EventWrapper>): Either<BaseError, PushNotification> {
        if (events.isEmpty()) {
            return InvalidEventHistoryError("Event history cannot be empty").left()
        }

        val creationEnvelope =
            events.find { it.payload is PushNotificationCreatedEvent }
                ?: return MissingCreationEventError("PushNotificationCreatedEvent").left()

        val creationEvent = creationEnvelope.payload as PushNotificationCreatedEvent

        val notification =
            PushNotification(
                content = creationEvent.content,
                payload = creationEvent.payload,
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                id = creationEnvelope.aggregateId,
                createdAt = creationEnvelope.occurredAt,
                updatedAt = creationEnvelope.occurredAt,
                deviceToken = creationEvent.deviceToken,
                title = creationEvent.title,
                data = creationEvent.data,
                isNew = false,
            )

        notification.loadFromHistory(events)

        return notification.right()
    }
}
