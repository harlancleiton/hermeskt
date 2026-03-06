package br.com.olympus.hermes.shared.infrastructure.messaging

import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.EventWrapper
import br.com.olympus.hermes.shared.domain.events.NotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.NotificationDeliveryFailedEvent
import br.com.olympus.hermes.shared.domain.events.NotificationSentEvent
import br.com.olympus.hermes.shared.domain.events.PushNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.DeviceToken
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
import java.util.Date

/**
 * Serializable envelope wrapping a [DomainEvent] for Kafka transport. The [payload] is a flat [Map]
 * of primitive-typed fields, avoiding value class serialization issues.
 *
 * @property eventType The simple class name of the domain event.
 * @property occurredAt The timestamp when the event was produced.
 * @property payload Flat map of the event fields, all values are primitive-safe types.
 */
data class KafkaEventWrapper(
    val eventType: String,
    val occurredAt: Date,
    val payload: Map<String, Any?>,
) {
    companion object {
        /**
         * Creates a [KafkaEventWrapper] from a [DomainEvent], flattening value objects into
         * primitive-safe fields.
         *
         * @param event The domain event to wrap.
         * @return A new [KafkaEventWrapper] instance.
         */
        fun from(wrapper: EventWrapper): KafkaEventWrapper =
            KafkaEventWrapper(
                eventType = wrapper.payload::class.simpleName ?: "UnknownEvent",
                occurredAt = wrapper.occurredAt,
                payload =
                    wrapper.payload.toMap(
                        aggregateId = wrapper.aggregateId.value.toString(),
                    ),
            )

        /**
         * Reconstructs a [NotificationCreatedEvent] from this wrapper's flat [payload] map. Returns
         * null if the [eventType] is not a supported [NotificationCreatedEvent] subtype.
         *
         * @return The reconstructed event or null.
         */
        @Suppress("UNCHECKED_CAST")
        fun KafkaEventWrapper.toNotificationCreatedEvent(): NotificationCreatedEvent? {
            val p = payload
            val aggregateId = p["aggregateId"] as? String ?: return null
            val content = p["content"] as? String ?: return null

            @Suppress("UNCHECKED_CAST")
            val eventPayload =
                ((p["payload"] as? Map<*, *>) ?: emptyMap<String, Any>()) as Map<String, Any>
            return when (eventType) {
                "EmailNotificationCreatedEvent" -> {
                    val from =
                        Email.from(p["from"] as? String ?: return null).getOrNull()
                            ?: return null
                    val to =
                        Email.from(p["to"] as? String ?: return null).getOrNull() ?: return null
                    val subject =
                        EmailSubject.create(p["subject"] as? String ?: return null).getOrNull()
                            ?: return null
                    EmailNotificationCreatedEvent(
                        aggregateId = aggregateId,
                        content = content,
                        payload = eventPayload,
                        from = from,
                        to = to,
                        subject = subject,
                    )
                }
                "SMSNotificationCreatedEvent" -> {
                    val from = (p["from"] as? Number)?.toInt()?.toUInt() ?: return null
                    val to =
                        BrazilianPhone.create(p["to"] as? String ?: return null).getOrNull()
                            ?: return null
                    SMSNotificationCreatedEvent(
                        aggregateId = aggregateId,
                        content = content,
                        payload = eventPayload,
                        from = from,
                        to = to,
                    )
                }
                "WhatsAppNotificationCreatedEvent" -> {
                    val from =
                        BrazilianPhone.create(p["from"] as? String ?: return null).getOrNull()
                            ?: return null
                    val to =
                        BrazilianPhone.create(p["to"] as? String ?: return null).getOrNull()
                            ?: return null
                    val templateName = p["templateName"] as? String ?: return null
                    WhatsAppNotificationCreatedEvent(
                        aggregateId = aggregateId,
                        content = content,
                        payload = eventPayload,
                        from = from,
                        to = to,
                        templateName = templateName,
                    )
                }
                "PushNotificationCreatedEvent" -> {
                    val deviceToken =
                        DeviceToken
                            .create(p["deviceToken"] as? String ?: return null)
                            .getOrNull()
                            ?: return null
                    val title = p["title"] as? String ?: return null

                    @Suppress("UNCHECKED_CAST")
                    val data =
                        (p["data"] as? Map<*, *>)?.mapKeys { it.key as String }?.mapValues {
                            it.value as String
                        }
                            ?: emptyMap()
                    PushNotificationCreatedEvent(
                        aggregateId = aggregateId,
                        content = content,
                        payload = eventPayload,
                        deviceToken = deviceToken,
                        title = title,
                        data = data,
                    )
                }
                else -> null
            }
        }

        /**
         * Reconstructs a [NotificationSentEvent] from this wrapper's flat [payload] map. Also
         * recovers the `aggregateId` embedded in the payload by [from]. Returns null if the
         * [eventType] does not match.
         *
         * @return The reconstructed event (with aggregateId in [KafkaEventWrapper.payload]) or
         * null.
         */
        fun KafkaEventWrapper.toNotificationSentEvent(): NotificationSentEvent? {
            if (eventType != "NotificationSentEvent") return null
            val p = payload
            val shippingReceipt = p["shippingReceipt"] ?: return null
            val sentAt = (p["sentAt"] as? Number)?.toLong()?.let { Date(it) } ?: return null
            return NotificationSentEvent(shippingReceipt = shippingReceipt, sentAt = sentAt)
        }

        /**
         * Reconstructs a [NotificationDeliveryFailedEvent] from this wrapper's flat [payload] map.
         * Returns null if the [eventType] does not match.
         *
         * @return The reconstructed event or null.
         */
        fun KafkaEventWrapper.toNotificationDeliveryFailedEvent(): NotificationDeliveryFailedEvent? {
            if (eventType != "NotificationDeliveryFailedEvent") return null
            val p = payload
            val aggregateId = p["aggregateId"] as? String ?: return null
            val reason = p["reason"] as? String ?: return null
            val failedAt = (p["failedAt"] as? Number)?.toLong()?.let { Date(it) } ?: Date()
            return NotificationDeliveryFailedEvent(
                aggregateId = aggregateId,
                reason = reason,
                failedAt = failedAt,
            )
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun DomainEvent.toMap(aggregateId: String = ""): Map<String, Any?> =
    when (this) {
        is EmailNotificationCreatedEvent ->
            mapOf(
                "aggregateId" to aggregateId,
                "content" to content,
                "payload" to payload,
                "from" to from.value,
                "to" to to.value,
                "subject" to subject.subject,
            )
        is SMSNotificationCreatedEvent ->
            mapOf(
                "aggregateId" to aggregateId,
                "content" to content,
                "payload" to payload,
                "from" to from.toInt(),
                "to" to to.value,
            )
        is WhatsAppNotificationCreatedEvent ->
            mapOf(
                "aggregateId" to aggregateId,
                "content" to content,
                "payload" to payload,
                "from" to from.value,
                "to" to to.value,
                "templateName" to templateName,
            )
        is PushNotificationCreatedEvent ->
            mapOf(
                "aggregateId" to aggregateId,
                "content" to content,
                "payload" to payload,
                "deviceToken" to deviceToken.value,
                "title" to title,
                "data" to data,
            )
        is NotificationSentEvent ->
            mapOf(
                "aggregateId" to aggregateId,
                "shippingReceipt" to shippingReceipt,
                "sentAt" to sentAt.time,
            )
        is NotificationDeliveryFailedEvent ->
            mapOf(
                "aggregateId" to this.aggregateId,
                "reason" to reason,
                "failedAt" to failedAt.time,
            )
        else -> emptyMap()
    }
