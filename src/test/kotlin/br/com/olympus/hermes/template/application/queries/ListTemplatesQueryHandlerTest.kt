package br.com.olympus.hermes.template.application.queries

import arrow.core.right
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.template.domain.entities.NotificationTemplate
import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import br.com.olympus.hermes.template.domain.valueobjects.TemplateBody
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class ListTemplatesQueryHandlerTest {
    private lateinit var repository: TemplateRepository
    private lateinit var handler: ListTemplatesQueryHandler

    @BeforeEach
    fun setUp() {
        repository = mockk()
        handler = ListTemplatesQueryHandler(repository)
    }

    private fun makeTemplate(
        name: String = "welcome-email",
        channel: NotificationType = NotificationType.EMAIL,
    ): NotificationTemplate =
        NotificationTemplate(
            name = TemplateName.create(name).getOrNull()!!,
            channel = channel,
            subject = null,
            body = TemplateBody.create("Hello.").getOrNull()!!,
            description = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @Test
    fun `should return paginated list`() {
        val templates = listOf(makeTemplate("t1"), makeTemplate("t2"))
        every { repository.findAllByChannel(null, 0, 20) } returns templates.right()

        val result = handler.handle(ListTemplatesQuery(channel = null, page = 0, size = 20))

        assert(result.isRight())
        result.onRight { assertEquals(2, it.size) }
    }

    @Test
    fun `should filter by channel`() {
        val templates = listOf(makeTemplate("sms-otp", NotificationType.SMS))
        every { repository.findAllByChannel(NotificationType.SMS, 0, 10) } returns templates.right()

        val result = handler.handle(ListTemplatesQuery(channel = "SMS", page = 0, size = 10))

        assert(result.isRight())
        result.onRight {
            assertEquals(1, it.size)
            assertTrue(it.all { t -> t.channel == NotificationType.SMS })
        }
    }

    @Test
    fun `should return empty list when no templates match`() {
        every { repository.findAllByChannel(any(), any(), any()) } returns
            emptyList<NotificationTemplate>().right()

        val result = handler.handle(ListTemplatesQuery(channel = "PUSH", page = 0, size = 20))

        assert(result.isRight())
        result.onRight { assertTrue(it.isEmpty()) }
    }
}
