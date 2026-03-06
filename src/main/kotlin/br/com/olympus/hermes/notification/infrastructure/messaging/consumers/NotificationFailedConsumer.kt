package br.com.olympus.hermes.notification.infrastructure.messaging.consumers

import br.com.olympus.hermes.notification.application.projectors.NotificationFailedProjector
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper.Companion.toNotificationDeliveryFailedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.logging.Log
import io.smallrye.common.annotation.Blocking
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming

/**
 * Kafka consumer for notification-delivery-failed events. Deserializes the raw JSON message into a
 * [KafkaEventWrapper] and delegates projection logic to [NotificationFailedProjector].
 *
 * @property projector The application-layer projector that updates `failureReason` in MongoDB.
 * @property objectMapper Jackson mapper used for JSON deserialization.
 */
@ApplicationScoped
class NotificationFailedConsumer(
    private val projector: NotificationFailedProjector,
    private val objectMapper: ObjectMapper,
) {
    /**
     * Consumes raw JSON messages from `hermes.notification.delivery-failed` and delegates to
     * [NotificationFailedProjector].
     *
     * Malformed messages are skipped with a warning. Projection failures are propagated as
     * [RuntimeException] so the framework performs a nack and routes to the dead-letter queue.
     *
     * @param json The raw JSON payload received from Kafka.
     */
    @Incoming("hermes-notification-failed-projector")
    @Blocking
    fun consume(json: String) {
        val wrapper =
            try {
                objectMapper.readValue(json, KafkaEventWrapper::class.java)
            } catch (ex: Exception) {
                Log.errorf(
                    ex,
                    "NotificationFailedConsumer: failed to deserialize message, skipping",
                )
                return
            }

        val event = wrapper.toNotificationDeliveryFailedEvent()
        if (event == null) {
            Log.warnf(
                "NotificationFailedConsumer: skipping unrecognised event type '%s'",
                wrapper.eventType,
            )
            return
        }

        projector.handle(event).onLeft { error ->
            throw RuntimeException(
                "NotificationFailedConsumer: projection failed for event '${wrapper.eventType}': ${error.message}",
            )
        }
    }
}
