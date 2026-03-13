package br.com.olympus.hermes.notification.infrastructure.messaging.consumers

import br.com.olympus.hermes.notification.application.ports.IncomingDomainEvent
import br.com.olympus.hermes.notification.application.ports.UpstreamDomainEventHandler
import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import io.smallrye.common.annotation.Blocking
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming

/**
 * Kafka consumer that ingests raw upstream domain events from external bounded contexts.
 *
 * Listens on the `hermes-domain-events-consumer` channel (mapped to topic `hermes.domain.events`)
 * and delegates each valid message to [UpstreamDomainEventHandler] for mounting and dispatch.
 *
 * **DLQ routing**: any unhandled exception propagated from this method causes SmallRye to nack the
 * message. Combined with `failure-strategy=dead-letter-queue` in `application.properties`, failed
 * messages are automatically routed to `hermes.domain.events-dead-letter-queue`.
 *
 * **Malformed JSON**: deserialization errors are logged and the message is silently skipped (no nack)
 * to avoid poisoning the consumer group with permanently unreadable payloads.
 *
 * @property handler Application-layer port that processes the mounted notification flow.
 * @property objectMapper Jackson mapper used for JSON deserialization.
 */
@ApplicationScoped
class GenericDomainEventConsumer(
    private val handler: UpstreamDomainEventHandler,
    private val objectMapper: ObjectMapper,
) {
    /**
     * Consumes raw JSON messages from the upstream domain events Kafka topic.
     *
     * Processing steps:
     * 1. Deserialize the JSON payload into a generic [Map].
     * 2. Extract `eventType`, `occurredAt`, and `payload` fields.
     * 3. Build an [IncomingDomainEvent] and delegate to [UpstreamDomainEventHandler].
     * 4. Propagate any [Either.Left] error as a [RuntimeException] to trigger a nack + DLQ routing.
     *
     * @param json The raw JSON payload received from Kafka.
     */
    @Incoming("hermes-domain-events-consumer")
    @Blocking
    @WithSpan("notification.upstream-event.consume")
    fun consume(json: String) {
        @Suppress("UNCHECKED_CAST")
        val raw: Map<String, Any?> =
            try {
                objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
            } catch (ex: Exception) {
                Log.warnf("GenericDomainEventConsumer: malformed JSON payload, skipping: %s", ex.message)
                return
            }

        val eventType = raw["eventType"] as? String
        if (eventType == null) {
            Log.warnf("GenericDomainEventConsumer: missing 'eventType' field, skipping message")
            return
        }

        @Suppress("UNCHECKED_CAST")
        val payload = (raw["payload"] as? Map<*, *>)?.mapKeys { it.key as String } ?: emptyMap()

        val occurredAt =
            (raw["occurredAt"] as? Number)
                ?.toLong()
                ?.let { java.util.Date(it) }
                ?: java.util.Date()

        Span.current().setAttribute("upstream.event.type", eventType)
        Log.infof("GenericDomainEventConsumer: received upstream event type=%s", eventType)

        val event = IncomingDomainEvent(eventType = eventType, occurredAt = occurredAt, payload = payload)

        handler.handle(event).onLeft { error ->
            throw RuntimeException(
                "GenericDomainEventConsumer: handler failed for event '$eventType': ${error.message}",
            )
        }
    }
}
