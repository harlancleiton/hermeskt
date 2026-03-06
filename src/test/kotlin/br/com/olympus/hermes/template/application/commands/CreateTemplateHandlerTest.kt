package br.com.olympus.hermes.template.application.commands

import arrow.core.right
import br.com.olympus.hermes.notification.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.TemplateDuplicateError
import br.com.olympus.hermes.template.domain.entities.NotificationTemplate
import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import br.com.olympus.hermes.template.domain.valueobjects.TemplateBody
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class CreateTemplateHandlerTest {
    private lateinit var repository: TemplateRepository
    private lateinit var handler: CreateTemplateHandler

    @BeforeEach
    fun setUp() {
        repository = mockk()
        handler = CreateTemplateHandler(repository)
    }

    private fun makeTemplate(name: String = "welcome-email"): NotificationTemplate =
        NotificationTemplate(
            name = TemplateName.create(name).getOrNull()!!,
            channel = NotificationType.EMAIL,
            subject = "Welcome!",
            body = TemplateBody.create("Hello {{name}}.").getOrNull()!!,
            description = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @Test
    fun `should create template successfully`() {
        val template = makeTemplate()
        every { repository.existsByNameAndChannel(any(), any()) } returns false.right()
        every { repository.save(any()) } returns template.right()

        val command =
            CreateTemplateCommand(
                name = "welcome-email",
                channel = "EMAIL",
                subject = "Welcome!",
                body = "Hello {{name}}.",
                description = null,
            )

        val result = handler.handle(command)

        assert(result.isRight())
        verify { repository.save(any()) }
    }

    @Test
    fun `should return TemplateDuplicateError when name and channel already exists`() {
        every { repository.existsByNameAndChannel(any(), any()) } returns true.right()

        val command =
            CreateTemplateCommand(
                name = "welcome-email",
                channel = "EMAIL",
                subject = "Welcome!",
                body = "Hello {{name}}.",
                description = null,
            )

        val result = handler.handle(command)

        assert(result.isLeft())
        result.onLeft { assertTrue(it is TemplateDuplicateError) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should validate TemplateName and TemplateBody VOs`() {
        val command =
            CreateTemplateCommand(
                name = "INVALID NAME!!",
                channel = "EMAIL",
                subject = null,
                body = "",
                description = null,
            )

        val result = handler.handle(command)

        assert(result.isLeft())
        verify(exactly = 0) { repository.existsByNameAndChannel(any(), any()) }
        verify(exactly = 0) { repository.save(any()) }
    }
}
