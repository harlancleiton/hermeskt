package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.NotificationDeliveredEvent
import br.com.olympus.hermes.shared.domain.events.NotificationSeenEvent
import br.com.olympus.hermes.shared.domain.events.NotificationSentEvent
import br.com.olympus.hermes.shared.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.Date

/** [EventPayloadCodec] for [EmailNotificationCreatedEvent]. */
object EmailNotificationCreatedCodec : EventPayloadCodec<EmailNotificationCreatedEvent> {
    override val eventType = "EmailNotificationCreatedEvent"

    override fun serialize(event: EmailNotificationCreatedEvent): Map<String, Any?> =
            mapOf(
                    "content" to event.content,
                    "payload" to event.payload,
                    "from" to event.from.value,
                    "to" to event.to.value,
                    "subject" to event.subject.subject,
            )

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            data: Map<String, Any>,
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
}

/** [EventPayloadCodec] for [SMSNotificationCreatedEvent]. */
object SMSNotificationCreatedCodec : EventPayloadCodec<SMSNotificationCreatedEvent> {
    override val eventType = "SMSNotificationCreatedEvent"

    override fun serialize(event: SMSNotificationCreatedEvent): Map<String, Any?> =
            mapOf(
                    "content" to event.content,
                    "payload" to event.payload,
                    "from" to event.from.toInt(),
                    "to" to event.to.value,
            )

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            data: Map<String, Any>,
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
}

/** [EventPayloadCodec] for [WhatsAppNotificationCreatedEvent]. */
object WhatsAppNotificationCreatedCodec : EventPayloadCodec<WhatsAppNotificationCreatedEvent> {
    override val eventType = "WhatsAppNotificationCreatedEvent"

    override fun serialize(event: WhatsAppNotificationCreatedEvent): Map<String, Any?> =
            mapOf(
                    "content" to event.content,
                    "payload" to event.payload,
                    "from" to event.from.value,
                    "to" to event.to.value,
                    "templateName" to event.templateName,
            )

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            data: Map<String, Any>,
    ): Either<BaseError, WhatsAppNotificationCreatedEvent> = either {
        val from = BrazilianPhone.create(data["from"] as String).bind()
        val to = BrazilianPhone.create(data["to"] as String).bind()
        WhatsAppNotificationCreatedEvent(
                id = eventId,
                aggregateId = aggregateId,
                aggregateVersion = version,
                occurredAt = occurredAt,
                content = data["content"] as String,
                payload = (data["payload"] as? Map<String, Any>) ?: emptyMap(),
                from = from,
                to = to,
                templateName = data["templateName"] as String,
        )
    }
}

/** [EventPayloadCodec] for [NotificationSentEvent]. */
object NotificationSentCodec : EventPayloadCodec<NotificationSentEvent> {
    override val eventType = "NotificationSentEvent"

    override fun serialize(event: NotificationSentEvent): Map<String, Any?> =
            mapOf("shippingReceipt" to event.shippingReceipt)

    override fun deserialize(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            data: Map<String, Any>,
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
}

/** [EventPayloadCodec] for [NotificationSeenEvent]. */
object NotificationSeenCodec : EventPayloadCodec<NotificationSeenEvent> {
    override val eventType = "NotificationSeenEvent"

    override fun serialize(event: NotificationSeenEvent): Map<String, Any?> = emptyMap()

    override fun deserialize(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            data: Map<String, Any>,
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
}

/** [EventPayloadCodec] for [NotificationDeliveredEvent]. */
object NotificationDeliveredCodec : EventPayloadCodec<NotificationDeliveredEvent> {
    override val eventType = "NotificationDeliveredEvent"

    override fun serialize(event: NotificationDeliveredEvent): Map<String, Any?> = emptyMap()

    override fun deserialize(
            eventId: EntityId,
            aggregateId: EntityId,
            version: Int,
            occurredAt: Date,
            data: Map<String, Any>,
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

/**
 * Builds and returns an [EventPayloadCodecRegistry] pre-populated with all known domain event
 * codecs.
 *
 * When a new [br.com.olympus.hermes.shared.domain.events.DomainEvent] is added, register its codec
 * here — no other file needs to change.
 */
fun defaultEventPayloadCodecRegistry(): EventPayloadCodecRegistry =
        EventPayloadCodecRegistry().apply {
            register(EmailNotificationCreatedCodec)
            register(SMSNotificationCreatedCodec)
            register(WhatsAppNotificationCreatedCodec)
            register(NotificationSentCodec)
            register(NotificationSeenCodec)
            register(NotificationDeliveredCodec)
        }
