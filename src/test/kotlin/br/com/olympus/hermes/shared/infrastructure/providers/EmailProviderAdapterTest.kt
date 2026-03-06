package br.com.olympus.hermes.shared.infrastructure.providers

import br.com.olympus.hermes.shared.domain.entities.EmailNotification
import br.com.olympus.hermes.shared.domain.exceptions.DeliveryError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import jakarta.mail.Message
import jakarta.mail.Transport
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EmailProviderAdapterTest {
    private lateinit var adapter: EmailProviderAdapter
    private val host = "smtp.example.com"
    private val port = 587
    private val username = "user"
    private val password = "password"
    private val startTls = true
    private val timeoutMs = 5000

    @BeforeEach
    fun setup() {
        adapter =
            EmailProviderAdapter(
                host = host,
                port = port,
                username = username,
                password = password,
                startTls = startTls,
                timeoutMs = timeoutMs,
            )
        mockkStatic(Transport::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should support EMAIL notification type`() {
        assertTrue(adapter.supports(NotificationType.EMAIL))
    }

    @Test
    fun `should send email successfully and return receipt`() {
        every { Transport.send(any()) } answers {}

        val notification =
            mockk<EmailNotification> {
                every { from } returns Email.from("sender@example.com").getOrNull()!!
                every { to } returns Email.from("recipient@example.com").getOrNull()!!
                every { subject } returns EmailSubject.create("Test Subject").getOrNull()!!
                every { content } returns "This is a test email."
            }

        val result = adapter.send(notification)

        assertTrue(result.isRight())
        result.onRight { receipt -> assertEquals("smtp", receipt.provider) }

        verify(exactly = 1) { Transport.send(any<Message>()) }
    }

    @Test
    fun `should return DeliveryError when Transport throws exception`() {
        val errorMessage = "SMTP connection failed"
        every { Transport.send(any()) } throws RuntimeException(errorMessage)

        val notification =
            mockk<EmailNotification> {
                every { from } returns Email.from("sender@example.com").getOrNull()!!
                every { to } returns Email.from("recipient@example.com").getOrNull()!!
                every { subject } returns EmailSubject.create("Test Subject").getOrNull()!!
                every { content } returns "This is a test email."
            }

        val result = adapter.send(notification)

        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertTrue(error is DeliveryError)
            assertEquals("Notification delivery failed: $errorMessage", error.message)
        }
    }
}
