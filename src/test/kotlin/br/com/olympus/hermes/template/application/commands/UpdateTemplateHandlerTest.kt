package br.com.olympus.hermes.template.application.commands

import arrow.core.right
import br.com.olympus.hermes.notification.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.TemplateNotFoundError
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

class UpdateTemplateHandlerTest {
    private lateinit var repository: TemplateRepository
    private lateinit var handler: UpdateTemplateHandler

    @BeforeEach
    fun setUp() {
        repository = mockk()
        handler = UpdateTemplateHandler(repository)
    }

    private fun makeTemplate(): NotificationTemplate =
        NotificationTemplate(
            name = TemplateName.create("welcome-email").getOrNull()!!,
            channel = NotificationType.EMAIL,
            subject = "Welcome!",
            body = TemplateBody.create("Hello {{name}}.").getOrNull()!!,
            description = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @Test
    fun `should update template successfully`() {
        val existing = makeTemplate()
        val updated =
            existing.copy(
                body = TemplateBody.create("Hi {{name}}, updated!").getOrNull()!!,
            )
        every { repository.findByNameAndChannel(any(), any()) } returns existing.right()
        every { repository.update(any()) } returns updated.right()

        val command =
            UpdateTemplateCommand(
                name = "welcome-email",
                channel = "EMAIL",
                subject = null,
                body = "Hi {{name}}, updated!",
                description = null,
            )

        val result = handler.handle(command)

        assert(result.isRight())
        verify { repository.update(any()) }
    }

    @Test
    fun `should return TemplateNotFoundError when template does not exist`() {
        every { repository.findByNameAndChannel(any(), any()) } returns null.right()

        val command =
            UpdateTemplateCommand(
                name = "nonexistent",
                channel = "EMAIL",
                subject = null,
                body = "New body.",
                description = null,
            )

        val result = handler.handle(command)

        assert(result.isLeft())
        result.onLeft { assertTrue(it is TemplateNotFoundError) }
        verify(exactly = 0) { repository.update(any()) }
    }
}
