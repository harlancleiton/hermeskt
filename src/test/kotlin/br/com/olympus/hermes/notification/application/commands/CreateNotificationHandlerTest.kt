package br.com.olympus.hermes.notification.application.commands

import arrow.core.Either
import br.com.olympus.hermes.notification.application.ports.ResolvedTemplateDto
import br.com.olympus.hermes.notification.application.ports.TemplateResolver
import br.com.olympus.hermes.notification.domain.entities.Notification
import br.com.olympus.hermes.notification.domain.factories.NotificationFactory
import br.com.olympus.hermes.notification.domain.factories.NotificationFactoryRegistry
import br.com.olympus.hermes.notification.domain.repositories.NotificationRepository
import br.com.olympus.hermes.shared.application.ports.DomainEventPublisher
import br.com.olympus.hermes.shared.domain.core.NotificationType
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class CreateNotificationHandlerTest {
    private val repository = mockk<NotificationRepository>()
    private val publisher = mockk<DomainEventPublisher>()
    private val factoryRegistry = mockk<NotificationFactoryRegistry>()
    private val templateResolver = mockk<TemplateResolver>()
    private val factory = mockk<NotificationFactory<Notification>>()

    private val handler =
        CreateNotificationHandler(repository, publisher, factoryRegistry, templateResolver)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `should create notification without template when templateName is null`() {
        // Given
        val command =
            CreateNotificationCommand.Sms(
                id = UUID.randomUUID().toString(),
                content = "Hello",
                from = 12345u,
                to = "+5511999999999",
                templateName = null,
                payload = emptyMap(),
            )
        val notification = mockk<Notification>(relaxed = true)

        every { factoryRegistry.getFactory<Notification>(NotificationType.SMS) } returns
            Either.Right(factory)
        every { factory.create(any()) } returns Either.Right(notification)
        every { repository.save(notification) } returns Either.Right(notification)
        every { notification.commit(publisher) } returns Either.Right(Unit)

        // When
        val result = handler.handle(command)

        // Then
        assert(result.isRight())
        verify(exactly = 0) { templateResolver.resolve(any(), any(), any()) }
        verify { repository.save(notification) }
        verify { notification.commit(publisher) }
    }

    @Test
    fun `should resolve template and create notification when templateName is provided`() {
        // Given
        val templateName = "welcome-sms"
        val payload = mapOf("name" to "Harlan")
        val command =
            CreateNotificationCommand.Sms(
                id = UUID.randomUUID().toString(),
                content = "", // Should be overwritten by template
                from = 12345u,
                to = "+5511999999999",
                templateName = templateName,
                payload = payload,
            )
        val resolvedTemplate = ResolvedTemplateDto(subject = null, body = "Welcome, Harlan!")
        val notification = mockk<Notification>(relaxed = true)

        every { templateResolver.resolve(templateName, NotificationType.SMS, payload) } returns
            Either.Right(resolvedTemplate)
        every { factoryRegistry.getFactory<Notification>(NotificationType.SMS) } returns
            Either.Right(factory)
        every { factory.create(any()) } returns Either.Right(notification)
        every { repository.save(notification) } returns Either.Right(notification)
        every { notification.commit(publisher) } returns Either.Right(Unit)

        // When
        val result = handler.handle(command)

        // Then
        assert(result.isRight())
        verify { templateResolver.resolve(templateName, NotificationType.SMS, payload) }
        verify { factory.create(any()) }
    }

    @Test
    fun `should resolve subject for email notifications`() {
        // Given
        val templateName = "welcome-email"
        val payload = mapOf("name" to "Harlan")
        val command =
            CreateNotificationCommand.Email(
                id = UUID.randomUUID().toString(),
                content = "",
                subject = "Default Subject",
                from = "sender@example.com",
                to = "recipient@example.com",
                templateName = templateName,
                payload = payload,
            )
        val resolvedTemplate = ResolvedTemplateDto(subject = "Welcome Subject", body = "Email Body")
        val notification = mockk<Notification>(relaxed = true)

        every { templateResolver.resolve(templateName, NotificationType.EMAIL, payload) } returns
            Either.Right(resolvedTemplate)
        every { factoryRegistry.getFactory<Notification>(NotificationType.EMAIL) } returns
            Either.Right(factory)
        every { factory.create(any()) } returns Either.Right(notification)
        every { repository.save(notification) } returns Either.Right(notification)
        every { notification.commit(publisher) } returns Either.Right(Unit)

        // When
        val result = handler.handle(command)

        // Then
        assert(result.isRight())
        verify { templateResolver.resolve(templateName, NotificationType.EMAIL, payload) }
        verify { factory.create(any()) }
    }
}
