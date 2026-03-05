package br.com.olympus.hermes.infrastructure.kafka.consumers

import br.com.olympus.hermes.core.application.projectors.NotificationCreatedProjector
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper.Companion.toNotificationCreatedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.logging.Log
import io.smallrye.common.annotation.Blocking
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming

/**
 * Kafka consumer for notification-created events. Deserializes the raw JSON message into a
 * [KafkaEventWrapper] and delegates projection logic to [NotificationCreatedProjector].
 *
 * @property projector The application-layer projector that maintains the MongoDB read model.
 * @property objectMapper Jackson mapper used for JSON deserialization.
 */
@ApplicationScoped
class NotificationCreatedConsumer(
    private val projector: NotificationCreatedProjector,
    private val objectMapper: ObjectMapper,
) {
    /**
     * Consumes raw JSON messages from the Kafka topic and delegates to [NotificationCreatedProjector].
     *
     * Malformed messages are skipped with a warning. Projection failures are propagated as
     * [RuntimeException] so the framework performs a nack and routes the message to the dead-letter
     * queue.
     *
     * @param json The raw JSON payload received from Kafka.
     */
    @Incoming("hermes-notification-created")
    @Blocking
    fun consume(json: String) {
        val wrapper =
            try {
                objectMapper.readValue(json, KafkaEventWrapper::class.java)
            } catch (ex: Exception) {
                Log.error("Failed to deserialize Kafka message, skipping: ${ex.message}", ex)
                return
            }
        val event = wrapper.toNotificationCreatedEvent()
        if (event == null) {
            Log.warn("Skipping unrecognised event type: ${wrapper.eventType}")
            return
        }
        projector.handle(event).onLeft { error ->
            throw RuntimeException(
                "Projection failed for event '${wrapper.eventType}': ${error.message}",
            )
        }
    }
}
