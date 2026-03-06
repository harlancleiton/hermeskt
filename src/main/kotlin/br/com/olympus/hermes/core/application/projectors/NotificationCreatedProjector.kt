package br.com.olympus.hermes.core.application.projectors

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.EventHandler
import br.com.olympus.hermes.shared.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.NotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.PushNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import java.util.Date

/**
 * Projector responsible for maintaining the [NotificationView] read model in MongoDB. Handles
 * [NotificationCreatedEvent] by upserting the corresponding view document. Idempotent:
 * re-processing the same event produces the same read-model state because the document id equals
 * the aggregate id.
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
    @WithSpan("notification.projector.apply")
    override fun handle(event: NotificationCreatedEvent): Either<BaseError, Unit> =
        either {
            Span.current().apply {
                setAttribute("notification.id", event.aggregateId)
                setAttribute("notification.type", event.type.name)
            }
            Log.info(
                "Projecting notification event aggregate=${event.aggregateId}" +
                    " type=${event.type}",
            )
            val view = toView(event).bind()
            viewRepository.upsert(view).bind()
            Span.current().setAttribute("notification.view.upserted", true)
        }

    private fun toView(event: NotificationCreatedEvent): Either<BaseError, NotificationView> =
        either {
            val now = Date()
            val view = NotificationView()
            view.id = event.aggregateId
            view.type = event.type.name
            view.content = event.content
            view.payload = event.payload
            view.status = "PENDING"
            view.createdAt = now
            view.updatedAt = now

            when (event) {
                is EmailNotificationCreatedEvent -> {
                    view.from = event.from.value
                    view.to = event.to.value
                    view.subject = event.subject.subject
                    Span.current().setAttribute("notification.channel", "EMAIL")
                }
                is SMSNotificationCreatedEvent -> {
                    view.from = event.from.toString()
                    view.to = event.to.value
                    Span.current().setAttribute("notification.channel", "SMS")
                }
                is WhatsAppNotificationCreatedEvent -> {
                    view.from = event.from.value
                    view.to = event.to.value
                    view.templateName = event.templateName
                    Span.current().apply {
                        setAttribute("notification.channel", "WHATSAPP")
                        setAttribute("notification.template.name", event.templateName)
                    }
                }
                is PushNotificationCreatedEvent -> {
                    view.deviceToken = event.deviceToken.value
                    view.title = event.title
                    Span.current().apply {
                        setAttribute("notification.channel", "PUSH")
                        setAttribute("notification.deviceToken", event.deviceToken.value)
                    }
                }
            }

            view
        }
}
