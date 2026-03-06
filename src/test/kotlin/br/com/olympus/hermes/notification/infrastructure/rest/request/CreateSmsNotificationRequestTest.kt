package br.com.olympus.hermes.notification.infrastructure.rest.request

import br.com.olympus.hermes.notification.application.commands.CreateNotificationCommand
import br.com.olympus.hermes.shared.domain.core.NotificationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CreateSmsNotificationRequestTest {
    @Test
    fun `toCommand should map content correctly`() {
        val request =
            CreateSmsNotificationRequest(
                content = "Hello SMS",
                from = 12345u,
                to = "11999887766",
            )

        val result = request.toCommand()

        result.onRight { command -> assertEquals("Hello SMS", command.content) }
        assert(result.isRight())
    }

    @Test
    fun `toCommand should map from as UInt`() {
        val request =
            CreateSmsNotificationRequest(
                content = "Hello SMS",
                from = 98765u,
                to = "11999887766",
            )

        val result = request.toCommand()

        result.onRight { command -> assertEquals(98765u, command.from) }
        assert(result.isRight())
    }

    @Test
    fun `toCommand should map to correctly`() {
        val request =
            CreateSmsNotificationRequest(
                content = "Hello SMS",
                from = 12345u,
                to = "11999887766",
            )

        val result = request.toCommand()

        result.onRight { command -> assertEquals("11999887766", command.to) }
        assert(result.isRight())
    }

    @Test
    fun `toCommand should map payload correctly`() {
        val payload = mapOf("key" to "value")
        val request =
            CreateSmsNotificationRequest(
                content = "Hello SMS",
                from = 12345u,
                to = "11999887766",
                payload = payload,
            )

        val result = request.toCommand()

        result.onRight { command -> assertEquals(payload, command.payload) }
        assert(result.isRight())
    }

    @Test
    fun `toCommand should set type to SMS`() {
        val request =
            CreateSmsNotificationRequest(
                content = "Hello SMS",
                from = 12345u,
                to = "11999887766",
            )

        val result = request.toCommand()

        result.onRight { command -> assertEquals(NotificationType.SMS, command.type) }
        assert(result.isRight())
    }

    @Test
    fun `toCommand should produce a Right with the correct command type`() {
        val request =
            CreateSmsNotificationRequest(
                content = "Hello SMS",
                from = 12345u,
                to = "11999887766",
            )

        val result = request.toCommand()

        result.onRight { command -> assert(command is CreateNotificationCommand.Sms) }
        assert(result.isRight())
    }
}
