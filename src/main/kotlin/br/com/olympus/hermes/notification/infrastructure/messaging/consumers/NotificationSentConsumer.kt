package br.com.olympus.hermes.notification.infrastructure.messaging.consumers

import br.com.olympus.hermes.notification.application.projectors.NotificationSentProjector
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper.Companion.toNotificationSentEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.logging.Log
import io.smallrye.common.annotation.Blocking
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming

/**
 * Kafka consumer for notification-sent events. Deserializes the raw JSON message into a
 * [KafkaEventWrapper] and delegates projection logic to [NotificationSentProjector].
 *
 * The `aggregateId` is expected to be present in the wrapper's payload map as set by
 * [br.com.olympus.hermes.notification.infrastructure.messaging.consumers.DeliveryHandler.handleSuccess].
 *
 * @property projector The application-layer projector that updates `sentAt` in MongoDB.
 * @property objectMapper Jackson mapper used for JSON deserialization.
 */
@ApplicationScoped
class NotificationSentConsumer(
    private val projector: NotificationSentProjector,
    private val objectMapper: ObjectMapper,
) {
    /**
     * Consumes raw JSON messages from `hermes.notification.sent` and delegates to
     * [NotificationSentProjector].
     *
     * Malformed messages are skipped with a warning. Projection failures are propagated as
     * [RuntimeException] so the framework performs a nack and routes to the dead-letter queue.
     *
     * @param json The raw JSON payload received from Kafka.
     */
    @Incoming("hermes-notification-sent-projector")
    @Blocking
    fun consume(json: String) {
        val wrapper =
            try {
                objectMapper.readValue(json, KafkaEventWrapper::class.java)
            } catch (ex: Exception) {
                Log.errorf(
                    ex,
                    "NotificationSentConsumer: failed to deserialize message, skipping",
                )
                return
            }

        val event = wrapper.toNotificationSentEvent()
        if (event == null) {
            Log.warnf(
                "NotificationSentConsumer: skipping unrecognised event type '%s'",
                wrapper.eventType,
            )
            return
        }

        // Inject aggregateId into shippingReceipt map so the projector can read it
        val eventWithAggregateId =
            event.copy(
                shippingReceipt =
                    mapOf(
                        "aggregateId" to
                            (wrapper.payload["aggregateId"] as? String ?: ""),
                        "original" to event.shippingReceipt,
                    ),
            )

        projector.handle(eventWithAggregateId).onLeft { error ->
            throw RuntimeException(
                "NotificationSentConsumer: projection failed for event '${wrapper.eventType}': ${error.message}",
            )
        }
    }
}
