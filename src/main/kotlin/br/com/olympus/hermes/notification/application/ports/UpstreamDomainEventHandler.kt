package br.com.olympus.hermes.notification.application.ports

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import java.util.Date

/**
 * Represents a raw upstream domain event received from an external Kafka topic.
 *
 * @property eventType The simple class name of the source domain event (e.g., `User2FACodeRequested`).
 * @property occurredAt The timestamp when the event was produced by the upstream context.
 * @property payload Flat map of the event fields as primitive-safe values.
 */
data class IncomingDomainEvent(
    val eventType: String,
    val occurredAt: Date,
    val payload: Map<String, Any?>,
)

/**
 * Application-layer port for handling upstream domain events ingested from external Kafka topics.
 *
 * Implementations (provided in f2/f3) are responsible for determining the correct template,
 * mounting the notification payload, and triggering dispatch.
 */
interface UpstreamDomainEventHandler {
    /**
     * Handles a deserialized upstream domain event.
     *
     * @param event The incoming domain event extracted from the Kafka message.
     * @return [Either.Right] on success, or [Either.Left] with a [BaseError] on failure.
     */
    fun handle(event: IncomingDomainEvent): Either<BaseError, Unit>
}
