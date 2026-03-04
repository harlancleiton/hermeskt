package br.com.olympus.hermes.shared.infrastructure.messaging

import arrow.core.Either
import arrow.core.right
import br.com.olympus.hermes.shared.application.ports.DomainEventPublisher
import br.com.olympus.hermes.shared.domain.events.EventWrapper
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.EventPublishingError
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.OnOverflow

/**
 * Kafka implementation of [DomainEventPublisher]. Serializes domain events to JSON and emits them
 * to the appropriate Kafka topic via SmallRye Reactive Messaging [Emitter]. Topic name is derived
 * from the event type name following the convention: `hermes.notification.{event-type}`.
 */
@ApplicationScoped
class KafkaDomainEventPublisher(
    private val objectMapper: ObjectMapper,
) : DomainEventPublisher {
    @Inject
    @Channel("hermes-domain-events")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 256)
    lateinit var emitter: Emitter<String>

    /**
     * Publishes a single domain event to Kafka.
     *
     * @param event The domain event to publish.
     * @return Either an [EventPublishingError] on failure or [Unit] on success.
     */
    override fun publish(event: EventWrapper): Either<BaseError, Unit> =
        Either
            .catch {
                val json = objectMapper.writeValueAsString(KafkaEventWrapper.from(event))
                emitter.send(json).toCompletableFuture().get()
                Unit
            }.mapLeft { EventPublishingError("Failed to publish event", it) }

    /**
     * Publishes a batch of domain events to Kafka in order.
     *
     * @param events The list of event wrappers to publish.
     * @return Either an [EventPublishingError] on the first failure or [Unit] on success.
     */
    override fun publishAll(events: List<EventWrapper>): Either<BaseError, Unit> {
        for (event in events) {
            publish(event).onLeft {
                return Either.Left(it)
            }
        }
        return Unit.right()
    }
}
