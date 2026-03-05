package br.com.olympus.hermes.core.application.projectors

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.EventHandler
import br.com.olympus.hermes.shared.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.NotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import java.util.Date

/**
 * Projector responsible for maintaining the [NotificationView] read model in MongoDB. Handles
 * [NotificationCreatedEvent] by upserting the corresponding view document. Idempotent: re-processing
 * the same event produces the same read-model state because the document id equals the aggregate id.
 *
 * @property viewRepository Port for upserting [NotificationView] documents.
 */
@ApplicationScoped
class NotificationCreatedProjector(
    private val viewRepository: NotificationViewRepository,
) : EventHandler<NotificationCreatedEvent> {
    /**
     * Projects a [NotificationCreatedEvent] into a [NotificationView] document in MongoDB.
     *
     * @param event The notification created domain event to project.
     * @return Either a [BaseError] on failure or [Unit] on success.
     */
    override fun handle(event: NotificationCreatedEvent): Either<BaseError, Unit> =
        either {
            Log.info("Projecting notification created event: ${event::class.simpleName}")
            val view = toView(event).bind()
            viewRepository.upsert(view).bind()
        }

    private fun toView(event: NotificationCreatedEvent): Either<BaseError, NotificationView> =
        either {
            val now = Date()
            val view = NotificationView()
            view.id = event.aggregateId
            view.type = event.type.name
            view.content = event.content
            view.payload = event.payload
            view.createdAt = now
            view.updatedAt = now

            when (event) {
                is EmailNotificationCreatedEvent -> {
                    view.from = event.from.value
                    view.to = event.to.value
                    view.subject = event.subject.subject
                }
                is SMSNotificationCreatedEvent -> {
                    view.from = event.from.toString()
                    view.to = event.to.value
                }
                is WhatsAppNotificationCreatedEvent -> {
                    view.from = event.from.value
                    view.to = event.to.value
                    view.templateName = event.templateName
                }
            }

            view
        }
}
