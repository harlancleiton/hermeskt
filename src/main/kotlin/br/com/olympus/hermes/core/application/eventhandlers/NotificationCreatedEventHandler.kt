package br.com.olympus.hermes.core.application.eventhandlers

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.EventHandler
import br.com.olympus.hermes.shared.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.NotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper
import br.com.olympus.hermes.shared.infrastructure.messaging.KafkaEventWrapper.Companion.toNotificationCreatedEvent
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.logging.Log
import io.smallrye.common.annotation.Blocking
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import java.util.Date

/**
 * Projector that listens to Kafka notification-created events and upserts the corresponding
 * [NotificationView] document in MongoDB. Idempotent: re-processing the same event produces the
 * same read-model state.
 */
@ApplicationScoped
class NotificationCreatedEventHandler(
    private val viewRepository: NotificationViewRepository,
    private val objectMapper: ObjectMapper,
) : EventHandler<NotificationCreatedEvent> {
    /**
     * Handles a [NotificationCreatedEvent] by creating or updating the [NotificationView] document
     * in MongoDB.
     *
     * @param event The notification created domain event to project.
     * @return Either a [BaseError] on failure or [Unit] on success.
     */
    override fun handle(event: NotificationCreatedEvent): Either<BaseError, Unit> =
        either {
            Log.info("Handling notification created event: ${event::class.simpleName}")
            val view = toView(event).bind()
            viewRepository.upsert(view).bind()
        }

    /**
     * Consumes raw JSON messages from the Kafka topic and delegates to [handle].
     *
     * @param json The raw JSON payload from Kafka.
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
        handle(event).onLeft { error ->
            throw RuntimeException(
                "Projection failed for event '${wrapper.eventType}': ${error.message}",
            )
        }
    }

    private fun toView(event: NotificationCreatedEvent): Either<BaseError, NotificationView> =
        either {
            val id = EntityId.generate()
            val now = Date()
            val view = NotificationView()
            view.id = id.value.toString()
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
