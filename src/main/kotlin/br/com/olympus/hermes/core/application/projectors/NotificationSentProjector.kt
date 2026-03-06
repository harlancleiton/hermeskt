package br.com.olympus.hermes.core.application.projectors

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.EventHandler
import br.com.olympus.hermes.shared.domain.events.NotificationSentEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.NotificationNotFoundError
import br.com.olympus.hermes.shared.domain.exceptions.ProjectionError
import br.com.olympus.hermes.shared.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

/**
 * Projector that updates the
 * [br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView] read model when a
 * [NotificationSentEvent] is consumed. Sets the `sentAt` timestamp on the existing view document,
 * indicating that the notification was successfully delivered by the provider.
 *
 * **Idempotency**: Re-processing the same event overwrites `sentAt` with the same value, leaving
 * the document in the same state.
 *
 * @property viewRepository Port for reading and upserting [NotificationView] documents.
 */
@ApplicationScoped
class NotificationSentProjector(
    private val viewRepository: NotificationViewRepository,
) : EventHandler<NotificationSentEvent> {
    /**
     * Projects a [NotificationSentEvent] by updating the `sentAt` field on the corresponding
     * [br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView] document.
     *
     * @param event The notification-sent domain event, carrying the aggregateId in the wrapper.
     * @return Either a [BaseError] on failure or [Unit] on success.
     */
    @WithSpan("notification.projector.sent")
    override fun handle(event: NotificationSentEvent): Either<BaseError, Unit> =
        either {
            // aggregateId is injected into the event by the KafkaEventWrapper payload
            val aggregateIdStr =
                (event.shippingReceipt as? Map<*, *>)?.get("aggregateId") as? String
                    ?: raise(
                        ProjectionError(
                            "NotificationSentEvent missing aggregateId in shippingReceipt",
                        ),
                    )

            Span.current().setAttribute("notification.id", aggregateIdStr)
            Log.infof(
                "NotificationSentProjector: projecting sentAt for notification id=%s",
                aggregateIdStr,
            )

            val entityId = EntityId.from(aggregateIdStr).bind()
            val view =
                viewRepository.findById(entityId).bind()
                    ?: raise(NotificationNotFoundError(aggregateIdStr))

            view.sentAt = event.sentAt
            viewRepository.upsert(view).bind()

            Span.current().setAttribute("notification.view.upserted", true)
            Log.infof(
                "NotificationSentProjector: updated sentAt for notification id=%s",
                aggregateIdStr,
            )
        }
}
