package br.com.olympus.hermes.template.application.queries

import arrow.core.right
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.template.domain.entities.NotificationTemplate
import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import br.com.olympus.hermes.template.domain.valueobjects.TemplateBody
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class GetTemplateQueryHandlerTest {
    private lateinit var repository: TemplateRepository
    private lateinit var handler: GetTemplateQueryHandler

    @BeforeEach
    fun setUp() {
        repository = mockk()
        handler = GetTemplateQueryHandler(repository)
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
    fun `should return template when found`() {
        val template = makeTemplate()
        every { repository.findByNameAndChannel(any(), any()) } returns template.right()

        val result = handler.handle(GetTemplateQuery(name = "welcome-email", channel = "EMAIL"))

        assert(result.isRight())
        result.onRight { assertNotNull(it) }
    }

    @Test
    fun `should return null when not found`() {
        every { repository.findByNameAndChannel(any(), any()) } returns null.right()

        val result = handler.handle(GetTemplateQuery(name = "nonexistent", channel = "EMAIL"))

        assert(result.isRight())
        result.onRight { assertNull(it) }
    }
}
