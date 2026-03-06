package br.com.olympus.hermes.infrastructure.kafka.consumers

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.ports.DomainEventPublisher
import br.com.olympus.hermes.shared.application.ports.NotificationProviderAdapter
import br.com.olympus.hermes.shared.application.ports.ProviderAdapterRegistry
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.events.NotificationDeliveryFailedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.factories.NotificationFactoryRegistry
import br.com.olympus.hermes.shared.domain.repositories.EventStore
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import br.com.olympus.hermes.shared.domain.valueobjects.ProviderReceipt
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper.Companion.toNotificationCreatedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import io.smallrye.common.annotation.Blocking
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.OnOverflow
import java.util.Date

/**
 * Kafka consumer that drives the asynchronous notification delivery pipeline. Listens on a
 * dedicated consumer group for
 * [br.com.olympus.hermes.shared.domain.events.NotificationCreatedEvent]s, reconstitutes the
 * aggregate from DynamoDB, delegates to the appropriate channel [NotificationProviderAdapter],
 * persists the resulting lifecycle event, and publishes it to a downstream Kafka topic.
 *
 * **Idempotency**: If the aggregate already has `sentAt != null` the delivery is skipped, making
 * re-processing safe under Kafka rebalances.
 *
 * **Retry / DLQ**: On any transient failure a [RuntimeException] is thrown so SmallRye nacks the
 * message and retries according to the configured backoff. After max retries the message is routed
 * to the dead-letter queue.
 *
 * @property eventStore Write-side event store for reconstitution and appending new events.
 * @property factoryRegistry Factory registry used to reconstitute the aggregate from its history.
 * @property adapterRegistry Registry that resolves the correct provider adapter per channel type.
 * @property eventPublisher Domain event publisher used to persist committed aggregate changes.
 * @property objectMapper Jackson mapper for JSON deserialization.
 */
@ApplicationScoped
class DeliveryHandler(
    private val eventStore: EventStore,
    private val factoryRegistry: NotificationFactoryRegistry,
    private val adapterRegistry: ProviderAdapterRegistry,
    private val eventPublisher: DomainEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    /** Emitter for successfully delivered notification events. */
    @Inject
    @Channel("hermes-notification-sent")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 256)
    lateinit var sentEmitter: Emitter<String>

    /** Emitter for permanently failed notification delivery events. */
    @Inject
    @Channel("hermes-notification-failed")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 256)
    lateinit var failedEmitter: Emitter<String>

    /**
     * Consumes raw JSON messages from the Kafka `hermes.notification.created` topic (delivery
     * group).
     *
     * Processing steps:
     * 1. Deserialize `KafkaEventWrapper` — skip malformed messages gracefully.
     * 2. Convert to `NotificationCreatedEvent` — skip and warn on unknown event types.
     * 3. Reconstitute the aggregate from DynamoDB EventStore.
     * 4. Idempotency guard: return early if `sentAt` is already set.
     * 5. Resolve the provider adapter for the notification type.
     * 6. Invoke the adapter. 7a. Success: persist `NotificationSentEvent` via EventStore and
     * publish to `hermes.notification.sent`. 7b. Failure: publish `NotificationDeliveryFailedEvent`
     * to `hermes.notification.delivery-failed`
     * ```
     *     and throw [RuntimeException] to trigger a nack.
     *
     * @param json
     * ```
     * The raw JSON payload received from Kafka.
     */
    @Incoming("hermes-notification-delivery")
    @Blocking
    @WithSpan("notification.delivery.handle")
    fun consume(json: String) {
        val wrapper =
            try {
                objectMapper.readValue(json, KafkaEventWrapper::class.java)
            } catch (ex: Exception) {
                Log.errorf(ex, "DeliveryHandler: failed to deserialize Kafka message, skipping")
                return
            }

        val createdEvent = wrapper.toNotificationCreatedEvent()
        if (createdEvent == null) {
            Log.warnf("DeliveryHandler: skipping unrecognised event type '%s'", wrapper.eventType)
            return
        }

        val aggregateId = createdEvent.aggregateId
        Span.current().setAttribute("notification.id", aggregateId)
        Span.current().setAttribute("notification.type", createdEvent.type.name)
        Log.infof(
            "DeliveryHandler: processing delivery for notification id=%s type=%s",
            aggregateId,
            createdEvent.type,
        )

        val entityId =
            EntityId
                .from(aggregateId)
                .fold(
                    ifLeft = {
                        Log.errorf(
                            "DeliveryHandler: invalid aggregateId '%s', skipping",
                            aggregateId,
                        )
                        return
                    },
                    ifRight = { it },
                )

        // Step 3: Reconstitute aggregate
        @Suppress("UNCHECKED_CAST")
        val reconstituted: Either<BaseError, Notification> =
            either {
                val events = eventStore.getEvents(entityId).bind()
                val factory = factoryRegistry.getFactory<Notification>(createdEvent.type).bind()
                factory.reconstitute(events).bind()
            }

        val notification: Notification =
            reconstituted.fold(
                ifLeft = { error ->
                    throw RuntimeException(
                        "DeliveryHandler: failed to reconstitute aggregate id=$aggregateId: ${error.message}",
                    )
                },
                ifRight = { it },
            )

        // Step 4: Idempotency guard
        if (notification.sentAt != null) {
            Log.infof(
                "DeliveryHandler: notification id=%s already sent, skipping (idempotent)",
                aggregateId,
            )
            return
        }

        // Step 5: Resolve adapter
        val adapter: NotificationProviderAdapter =
            adapterRegistry
                .getAdapter(createdEvent.type)
                .fold(
                    ifLeft = { error ->
                        throw RuntimeException(
                            "DeliveryHandler: no adapter for type ${createdEvent.type}: ${error.message}",
                        )
                    },
                    ifRight = { it },
                )

        // Step 6 & 7: Invoke adapter and handle result
        val sendResult = adapter.send(notification)
        sendResult.fold(
            ifLeft = { error -> handleFailure(aggregateId, error.message) },
            ifRight = { receipt -> handleSuccess(notification, receipt, aggregateId) },
        )
    }

    private fun handleSuccess(
        notification: Notification,
        receipt: ProviderReceipt,
        aggregateId: String,
    ) {
        notification.markAsSent(receipt)
        val uncommitted = notification.uncommittedChanges
        val previousVersion = notification.version - uncommitted.size

        val persistResult: Either<BaseError, Unit> =
            eventStore
                .append(
                    aggregateId = notification.id,
                    events = uncommitted,
                    expectedVersion = previousVersion,
                ).flatMap { eventPublisher.publishAll(uncommitted) }

        persistResult.fold(
            ifLeft = { error ->
                throw RuntimeException(
                    "DeliveryHandler: failed to persist/publish sent event for id=$aggregateId: ${error.message}",
                )
            },
            ifRight = {},
        )

        notification.uncommit()

        Span.current().setAttribute("notification.delivered", true)
        Log.infof(
            "DeliveryHandler: successfully delivered notification id=%s provider=%s receiptId=%s",
            aggregateId,
            receipt.provider,
            receipt.receiptId,
        )
    }

    private fun handleFailure(
        aggregateId: String,
        reason: String,
    ) {
        val failedEvent =
            NotificationDeliveryFailedEvent(
                aggregateId = aggregateId,
                reason = reason,
                failedAt = Date(),
            )
        try {
            val failedWrapper =
                KafkaEventWrapper(
                    eventType =
                        failedEvent::class.simpleName
                            ?: "NotificationDeliveryFailedEvent",
                    occurredAt = failedEvent.failedAt,
                    payload =
                        mapOf(
                            "aggregateId" to failedEvent.aggregateId,
                            "reason" to failedEvent.reason,
                            "failedAt" to failedEvent.failedAt.time,
                        ),
                )
            val failedJson = objectMapper.writeValueAsString(failedWrapper)
            failedEmitter.send(failedJson).toCompletableFuture().get()
        } catch (ex: Exception) {
            Log.errorf(
                ex,
                "DeliveryHandler: failed to publish NotificationDeliveryFailedEvent for id=%s",
                aggregateId,
            )
        }

        Span.current().setAttribute("notification.delivered", false)
        throw RuntimeException(
            "DeliveryHandler: provider error for notification id=$aggregateId: $reason",
        )
    }
}
