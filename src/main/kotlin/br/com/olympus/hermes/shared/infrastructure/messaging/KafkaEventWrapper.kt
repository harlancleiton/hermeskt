package br.com.olympus.hermes.shared.infrastructure.messaging

import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.NotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
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
        fun from(event: DomainEvent): KafkaEventWrapper =
            KafkaEventWrapper(
                eventType = event::class.simpleName ?: "UnknownEvent",
                occurredAt = Date(),
                payload = event.toMap(),
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
                        content = content,
                        payload = eventPayload,
                        from = from,
                        to = to,
                        templateName = templateName,
                    )
                }
                else -> null
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun DomainEvent.toMap(): Map<String, Any?> =
    when (this) {
        is EmailNotificationCreatedEvent ->
            mapOf(
                "content" to content,
                "payload" to payload,
                "from" to from.value,
                "to" to to.value,
                "subject" to subject.subject,
            )
        is SMSNotificationCreatedEvent ->
            mapOf(
                "content" to content,
                "payload" to payload,
                "from" to from.toInt(),
                "to" to to.value,
            )
        is WhatsAppNotificationCreatedEvent ->
            mapOf(
                "content" to content,
                "payload" to payload,
                "from" to from.value,
                "to" to to.value,
                "templateName" to templateName,
            )
        else -> emptyMap()
    }
