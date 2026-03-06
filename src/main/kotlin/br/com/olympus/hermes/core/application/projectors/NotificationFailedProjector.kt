package br.com.olympus.hermes.core.application.projectors

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.EventHandler
import br.com.olympus.hermes.shared.domain.events.NotificationDeliveryFailedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.NotificationNotFoundError
import br.com.olympus.hermes.shared.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

/**
 * Projector that updates the
 * [br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView] read model when a
 * [NotificationDeliveryFailedEvent] is consumed from Kafka. Sets the `failureReason` field on the
 * existing view document.
 *
 * **Idempotency**: Re-processing the same event overwrites `failureReason` with the same value,
 * leaving the document in the same state.
 *
 * @property viewRepository Port for reading and upserting [NotificationView] documents.
 */
@ApplicationScoped
class NotificationFailedProjector(
    private val viewRepository: NotificationViewRepository,
) : EventHandler<NotificationDeliveryFailedEvent> {
    /**
     * Projects a [NotificationDeliveryFailedEvent] by setting the `failureReason` field on the
     * corresponding [br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView].
     *
     * @param event The delivery-failed domain event.
     * @return Either a [BaseError] on failure or [Unit] on success.
     */
    @WithSpan("notification.projector.failed")
    override fun handle(event: NotificationDeliveryFailedEvent): Either<BaseError, Unit> =
        either {
            Span.current().apply {
                setAttribute("notification.id", event.aggregateId)
                setAttribute("notification.failure.reason", event.reason)
            }
            Log.infof(
                "NotificationFailedProjector: projecting failureReason for notification id=%s reason=%s",
                event.aggregateId,
                event.reason,
            )

            val entityId = EntityId.from(event.aggregateId).bind()
            val view =
                viewRepository.findById(entityId).bind()
                    ?: raise(NotificationNotFoundError(event.aggregateId))

            view.failureReason = event.reason
            viewRepository.upsert(view).bind()

            Span.current().setAttribute("notification.view.upserted", true)
            Log.infof(
                "NotificationFailedProjector: updated failureReason for notification id=%s",
                event.aggregateId,
            )
        }
}
