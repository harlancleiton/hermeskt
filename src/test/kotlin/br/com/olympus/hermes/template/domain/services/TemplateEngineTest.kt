package br.com.olympus.hermes.template.domain.services

import arrow.core.left
import arrow.core.right
import br.com.olympus.hermes.notification.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.MissingTemplateVariablesError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateNotFoundError
import br.com.olympus.hermes.template.domain.entities.NotificationTemplate
import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import br.com.olympus.hermes.template.domain.valueobjects.TemplateBody
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class TemplateEngineTest {
    private lateinit var repository: TemplateRepository
    private lateinit var engine: TemplateEngine
    private val placeholderRegex = Regex("""\{\{(\w[\w.]*)\}\}""")

    @BeforeEach
    fun setUp() {
        repository = mockk()
        engine = TemplateEngine(repository, placeholderRegex)
    }

    private fun makeTemplate(
        body: String,
        subject: String? = null,
        channel: NotificationType = NotificationType.EMAIL,
        name: String = "test-template",
    ): NotificationTemplate =
        NotificationTemplate(
            name = TemplateName.create(name).getOrNull()!!,
            channel = channel,
            subject = subject,
            body = TemplateBody.create(body).getOrNull()!!,
            description = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @Test
    fun `resolve should interpolate all placeholders`() {
        val template = makeTemplate("Hello {{name}}, order {{orderId}} confirmed.")
        every { repository.findByNameAndChannel(any(), any()) } returns template.right()

        val result =
            engine.resolve(
                TemplateName.create("test-template").getOrNull()!!,
                NotificationType.EMAIL,
                mapOf("name" to "John", "orderId" to "123"),
            )

        assert(result.isRight())
        result.onRight { assertEquals("Hello John, order 123 confirmed.", it.body) }
    }

    @Test
    fun `resolve should interpolate subject for EMAIL channel`() {
        val template = makeTemplate("Hello {{name}}.", subject = "Order {{orderId}} confirmed")
        every { repository.findByNameAndChannel(any(), any()) } returns template.right()

        val result =
            engine.resolve(
                TemplateName.create("test-template").getOrNull()!!,
                NotificationType.EMAIL,
                mapOf("name" to "John", "orderId" to "42"),
            )

        assert(result.isRight())
        result.onRight {
            assertEquals("Hello John.", it.body)
            assertEquals("Order 42 confirmed", it.subject)
        }
    }

    @Test
    fun `resolve should return TemplateNotFoundError when template missing`() {
        every { repository.findByNameAndChannel(any(), any()) } returns null.right()

        val result =
            engine.resolve(
                TemplateName.create("nonexistent").getOrNull()!!,
                NotificationType.SMS,
                emptyMap(),
            )

        assert(result.isLeft())
        result.onLeft { assert(it is TemplateNotFoundError) }
    }

    @Test
    fun `resolve should return MissingTemplateVariablesError when payload incomplete`() {
        val template = makeTemplate("Hello {{name}}, your code is {{code}}.")
        every { repository.findByNameAndChannel(any(), any()) } returns template.right()

        val result =
            engine.resolve(
                TemplateName.create("test-template").getOrNull()!!,
                NotificationType.EMAIL,
                mapOf("name" to "John"),
            )

        assert(result.isLeft())
        result.onLeft {
            assert(it is MissingTemplateVariablesError)
            it as MissingTemplateVariablesError
            assert("code" in it.missing)
        }
    }

    @Test
    fun `resolve should list ALL missing variables, not just first`() {
        val template = makeTemplate("{{a}} {{b}} {{c}}")
        every { repository.findByNameAndChannel(any(), any()) } returns template.right()

        val result =
            engine.resolve(
                TemplateName.create("test-template").getOrNull()!!,
                NotificationType.SMS,
                emptyMap(),
            )

        assert(result.isLeft())
        result.onLeft {
            it as MissingTemplateVariablesError
            assertEquals(3, it.missing.size)
            assert("a" in it.missing)
            assert("b" in it.missing)
            assert("c" in it.missing)
        }
    }

    @Test
    fun `resolve should succeed when payload has extra keys beyond placeholders`() {
        val template = makeTemplate("Hello {{name}}.")
        every { repository.findByNameAndChannel(any(), any()) } returns template.right()

        val result =
            engine.resolve(
                TemplateName.create("test-template").getOrNull()!!,
                NotificationType.EMAIL,
                mapOf("name" to "John", "extra" to "ignored", "anotherExtra" to 42),
            )

        assert(result.isRight())
        result.onRight { assertEquals("Hello John.", it.body) }
    }

    @Test
    fun `resolve should return body unchanged when no placeholders present`() {
        val template = makeTemplate("No variables here.")
        every { repository.findByNameAndChannel(any(), any()) } returns template.right()

        val result =
            engine.resolve(
                TemplateName.create("test-template").getOrNull()!!,
                NotificationType.PUSH,
                emptyMap(),
            )

        assert(result.isRight())
        result.onRight { assertEquals("No variables here.", it.body) }
    }

    @Test
    fun `resolve should handle nested dot-notation variable names`() {
        val template = makeTemplate("Hello {{user.name}}, order {{order.id}}.")
        every { repository.findByNameAndChannel(any(), any()) } returns template.right()

        val result =
            engine.resolve(
                TemplateName.create("test-template").getOrNull()!!,
                NotificationType.EMAIL,
                mapOf("user.name" to "Alice", "order.id" to "999"),
            )

        assert(result.isRight())
        result.onRight { assertEquals("Hello Alice, order 999.", it.body) }
    }

    @Test
    fun `resolve should not fail on literal double-braces that are not valid placeholders`() {
        // {{ alone or {{123invalid}} won't match the regex — body passes through unchanged
        val template = makeTemplate("Price: {{ special }}")
        every { repository.findByNameAndChannel(any(), any()) } returns template.right()

        val result =
            engine.resolve(
                TemplateName.create("test-template").getOrNull()!!,
                NotificationType.EMAIL,
                emptyMap(),
            )

        assert(result.isRight())
        result.onRight { assertEquals("Price: {{ special }}", it.body) }
    }

    @Test
    fun `resolve should propagate repository errors`() {
        every { repository.findByNameAndChannel(any(), any()) } returns
            PersistenceError("DB down").left()

        val result =
            engine.resolve(
                TemplateName.create("test-template").getOrNull()!!,
                NotificationType.EMAIL,
                emptyMap(),
            )

        assert(result.isLeft())
        result.onLeft { assert(it is PersistenceError) }
    }
}
