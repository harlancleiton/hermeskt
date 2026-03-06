package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.NotificationDeliveredEvent
import br.com.olympus.hermes.shared.domain.events.NotificationDeliveryFailedEvent
import br.com.olympus.hermes.shared.domain.events.NotificationSeenEvent
import br.com.olympus.hermes.shared.domain.events.NotificationSentEvent
import br.com.olympus.hermes.shared.domain.events.PushNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.DeviceToken
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
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
        data: Map<String, Any>,
        aggregateId: String,
    ): Either<BaseError, EmailNotificationCreatedEvent> =
        either {
            val from = Email.from(data["from"] as String).bind()
            val to = Email.from(data["to"] as String).bind()
            val subject = EmailSubject.create(data["subject"] as String).bind()
            EmailNotificationCreatedEvent(
                aggregateId = aggregateId,
                content = data["content"] as String,
                payload = (data["payload"] as? Map<String, Any>) ?: emptyMap(),
                from = from,
                to = to,
                subject = subject,
            )
        }
}

/** [EventPayloadCodec] for [PushNotificationCreatedEvent]. */
object PushNotificationCreatedCodec : EventPayloadCodec<PushNotificationCreatedEvent> {
    override val eventType = "PushNotificationCreatedEvent"

    override fun serialize(event: PushNotificationCreatedEvent): Map<String, Any?> =
        mapOf(
            "content" to event.content,
            "payload" to event.payload,
            "deviceToken" to event.deviceToken.value,
            "title" to event.title,
            "data" to event.data,
        )

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(
        data: Map<String, Any>,
        aggregateId: String,
    ): Either<BaseError, PushNotificationCreatedEvent> =
        either {
            val deviceToken = DeviceToken.create(data["deviceToken"] as String).bind()
            PushNotificationCreatedEvent(
                aggregateId = aggregateId,
                content = data["content"] as String,
                payload = (data["payload"] as? Map<String, Any>) ?: emptyMap(),
                deviceToken = deviceToken,
                title = data["title"] as String,
                data = (data["data"] as? Map<String, String>) ?: emptyMap(),
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
        data: Map<String, Any>,
        aggregateId: String,
    ): Either<BaseError, SMSNotificationCreatedEvent> =
        either {
            val to = BrazilianPhone.create(data["to"] as String).bind()
            SMSNotificationCreatedEvent(
                aggregateId = aggregateId,
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
        data: Map<String, Any>,
        aggregateId: String,
    ): Either<BaseError, WhatsAppNotificationCreatedEvent> =
        either {
            val from = BrazilianPhone.create(data["from"] as String).bind()
            val to = BrazilianPhone.create(data["to"] as String).bind()
            WhatsAppNotificationCreatedEvent(
                aggregateId = aggregateId,
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
        mapOf(
            "shippingReceipt" to event.shippingReceipt,
            "sentAt" to event.sentAt.time,
        )

    override fun deserialize(
        data: Map<String, Any>,
        aggregateId: String,
    ): Either<BaseError, NotificationSentEvent> =
        Either
            .catch {
                NotificationSentEvent(
                    shippingReceipt = data["shippingReceipt"] ?: "",
                    sentAt =
                        Date(
                            (data["sentAt"] as? Number)?.toLong()
                                ?: System.currentTimeMillis(),
                        ),
                )
            }.mapLeft { PersistenceError("Failed to deserialize NotificationSentEvent", it) }
}

/** [EventPayloadCodec] for [NotificationSeenEvent]. */
object NotificationSeenCodec : EventPayloadCodec<NotificationSeenEvent> {
    override val eventType = "NotificationSeenEvent"

    override fun serialize(event: NotificationSeenEvent): Map<String, Any?> = mapOf("seenAt" to event.seenAt.time)

    override fun deserialize(
        data: Map<String, Any>,
        aggregateId: String,
    ): Either<BaseError, NotificationSeenEvent> =
        Either
            .catch {
                NotificationSeenEvent(
                    seenAt =
                        Date(
                            (data["seenAt"] as? Number)?.toLong()
                                ?: System.currentTimeMillis(),
                        ),
                )
            }.mapLeft { PersistenceError("Failed to deserialize NotificationSeenEvent", it) }
}

/** [EventPayloadCodec] for [NotificationDeliveredEvent]. */
object NotificationDeliveredCodec : EventPayloadCodec<NotificationDeliveredEvent> {
    override val eventType = "NotificationDeliveredEvent"

    override fun serialize(event: NotificationDeliveredEvent): Map<String, Any?> =
        mapOf("deliveredAt" to event.deliveredAt.time)

    override fun deserialize(
        data: Map<String, Any>,
        aggregateId: String,
    ): Either<BaseError, NotificationDeliveredEvent> =
        Either
            .catch {
                NotificationDeliveredEvent(
                    deliveredAt =
                        Date(
                            (data["deliveredAt"] as? Number)?.toLong()
                                ?: System.currentTimeMillis(),
                        ),
                )
            }.mapLeft {
                PersistenceError("Failed to deserialize NotificationDeliveredEvent", it)
            }
}

/** [EventPayloadCodec] for [NotificationDeliveryFailedEvent]. */
object NotificationDeliveryFailedCodec : EventPayloadCodec<NotificationDeliveryFailedEvent> {
    override val eventType = "NotificationDeliveryFailedEvent"

    override fun serialize(event: NotificationDeliveryFailedEvent): Map<String, Any?> =
        mapOf(
            "aggregateId" to event.aggregateId,
            "reason" to event.reason,
            "failedAt" to event.failedAt.time,
        )

    override fun deserialize(
        data: Map<String, Any>,
        aggregateId: String,
    ): Either<BaseError, NotificationDeliveryFailedEvent> =
        Either
            .catch {
                NotificationDeliveryFailedEvent(
                    aggregateId = data["aggregateId"] as? String ?: aggregateId,
                    reason = data["reason"] as? String ?: "",
                    failedAt =
                        Date(
                            (data["failedAt"] as? Number)?.toLong()
                                ?: System.currentTimeMillis(),
                        ),
                )
            }.mapLeft {
                PersistenceError(
                    "Failed to deserialize NotificationDeliveryFailedEvent",
                    it,
                )
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
        register(PushNotificationCreatedCodec)
        register(NotificationSentCodec)
        register(NotificationSeenCodec)
        register(NotificationDeliveredCodec)
        register(NotificationDeliveryFailedCodec)
    }
