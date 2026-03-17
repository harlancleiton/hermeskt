package br.com.olympus.hermes.notification.application.projectors

import arrow.core.Either
import br.com.olympus.hermes.notification.domain.events.EmailNotificationCreatedEvent
import br.com.olympus.hermes.notification.domain.events.PushNotificationCreatedEvent
import br.com.olympus.hermes.notification.domain.events.SMSNotificationCreatedEvent
import br.com.olympus.hermes.notification.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.notification.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.notification.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.notification.domain.valueobjects.DeviceToken
import br.com.olympus.hermes.notification.domain.valueobjects.Email
import br.com.olympus.hermes.notification.domain.valueobjects.EmailSubject
import br.com.olympus.hermes.shared.domain.exceptions.ProjectionError
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class NotificationCreatedProjectorTest {
    private val viewRepository: NotificationViewRepository = mockk()
    private val projector = NotificationCreatedProjector(viewRepository)

    @Test
    fun `should project EmailNotificationCreatedEvent with correct view fields`() {
        val aggregateId = UUID.randomUUID().toString()
        val from = Email.from("sender@example.com").getOrNull()!!
        val to = Email.from("recipient@example.com").getOrNull()!!
        val subject = EmailSubject.create("Welcome!").getOrNull()!!
        val event =
            EmailNotificationCreatedEvent(
                aggregateId = aggregateId,
                content = "Email body",
                payload = mapOf("name" to "Alice"),
                from = from,
                to = to,
                subject = subject,
            )

        every { viewRepository.upsert(any()) } returns Either.Right(Unit)

        val result = projector.handle(event)

        assertTrue(result.isRight())
        verify {
            viewRepository.upsert(
                match { view ->
                    view.id == aggregateId &&
                        view.type == "EMAIL" &&
                        view.from == "sender@example.com" &&
                        view.to == "recipient@example.com" &&
                        view.subject == "Welcome!" &&
                        view.status == "PENDING" &&
                        view.content == "Email body"
                },
            )
        }
    }

    @Test
    fun `should project SMSNotificationCreatedEvent with correct view fields`() {
        val aggregateId = UUID.randomUUID().toString()
        val to = BrazilianPhone.create("11999999999").getOrNull()!!
        val event =
            SMSNotificationCreatedEvent(
                aggregateId = aggregateId,
                content = "SMS body",
                payload = emptyMap(),
                from = 12345u,
                to = to,
            )

        every { viewRepository.upsert(any()) } returns Either.Right(Unit)

        val result = projector.handle(event)

        assertTrue(result.isRight())
        verify {
            viewRepository.upsert(
                match { view ->
                    view.id == aggregateId &&
                        view.type == "SMS" &&
                        view.from == "12345" &&
                        view.to == "11999999999" &&
                        view.status == "PENDING"
                },
            )
        }
    }

    @Test
    fun `should project WhatsAppNotificationCreatedEvent with correct view fields`() {
        val aggregateId = UUID.randomUUID().toString()
        val from = BrazilianPhone.create("11888888888").getOrNull()!!
        val to = BrazilianPhone.create("11777777777").getOrNull()!!
        val event =
            WhatsAppNotificationCreatedEvent(
                aggregateId = aggregateId,
                content = "WhatsApp body",
                payload = emptyMap(),
                from = from,
                to = to,
                templateName = "otp-template",
            )

        every { viewRepository.upsert(any()) } returns Either.Right(Unit)

        val result = projector.handle(event)

        assertTrue(result.isRight())
        verify {
            viewRepository.upsert(
                match { view ->
                    view.id == aggregateId &&
                        view.type == "WHATSAPP" &&
                        view.from == "11888888888" &&
                        view.to == "11777777777" &&
                        view.templateName == "otp-template" &&
                        view.status == "PENDING"
                },
            )
        }
    }

    @Test
    fun `should project PushNotificationCreatedEvent with correct view fields`() {
        val aggregateId = UUID.randomUUID().toString()
        val deviceToken = DeviceToken.create("device-abc-123").getOrNull()!!
        val event =
            PushNotificationCreatedEvent(
                aggregateId = aggregateId,
                content = "Push body",
                payload = emptyMap(),
                deviceToken = deviceToken,
                title = "Alert",
                data = mapOf("action" to "open"),
            )

        every { viewRepository.upsert(any()) } returns Either.Right(Unit)

        val result = projector.handle(event)

        assertTrue(result.isRight())
        verify {
            viewRepository.upsert(
                match { view ->
                    view.id == aggregateId &&
                        view.type == "PUSH" &&
                        view.deviceToken == "device-abc-123" &&
                        view.title == "Alert" &&
                        view.status == "PENDING"
                },
            )
        }
    }

    @Test
    fun `should return Left when repository upsert fails`() {
        val aggregateId = UUID.randomUUID().toString()
        val deviceToken = DeviceToken.create("token-xyz").getOrNull()!!
        val event =
            PushNotificationCreatedEvent(
                aggregateId = aggregateId,
                content = "Push body",
                payload = emptyMap(),
                deviceToken = deviceToken,
                title = "Title",
                data = emptyMap(),
            )
        val error = ProjectionError("MongoDB upsert failed")

        every { viewRepository.upsert(any()) } returns Either.Left(error)

        val result = projector.handle(event)

        assertTrue(result.isLeft())
        assertEquals(error, result.leftOrNull())
    }

    @Test
    fun `should be idempotent — re-processing same event yields same state`() {
        val aggregateId = UUID.randomUUID().toString()
        val deviceToken = DeviceToken.create("idempotent-token").getOrNull()!!
        val event =
            PushNotificationCreatedEvent(
                aggregateId = aggregateId,
                content = "body",
                payload = emptyMap(),
                deviceToken = deviceToken,
                title = "title",
                data = emptyMap(),
            )

        every { viewRepository.upsert(any()) } returns Either.Right(Unit)

        projector.handle(event)
        val result = projector.handle(event)

        assertTrue(result.isRight())
        verify(exactly = 2) { viewRepository.upsert(match { it.id == aggregateId }) }
    }
}
