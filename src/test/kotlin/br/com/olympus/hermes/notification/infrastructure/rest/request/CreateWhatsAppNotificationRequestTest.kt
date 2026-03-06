package br.com.olympus.hermes.notification.infrastructure.rest.request

import br.com.olympus.hermes.notification.application.commands.CreateNotificationCommand
import br.com.olympus.hermes.shared.domain.core.NotificationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CreateWhatsAppNotificationRequestTest {
    @Test
    fun `toCommand should map all fields correctly`() {
        val payload = mapOf("key" to "value")
        val request =
            CreateWhatsAppNotificationRequest(
                content = "Hello WhatsApp",
                from = "11999887766",
                to = "11988776655",
                templateName = "order_confirmation",
                payload = payload,
                notificationTemplateName = "hermes-template",
            )

        val result = request.toCommand()

        result.onRight { command ->
            assertEquals("Hello WhatsApp", command.content)
            assertEquals("11999887766", command.from)
            assertEquals("11988776655", command.to)
            assertEquals("order_confirmation", command.templateName)
            assertEquals(payload, command.payload)
        }
        assert(result.isRight())
    }

    @Test
    fun `toCommand should set type to WHATSAPP`() {
        val request =
            CreateWhatsAppNotificationRequest(
                content = "Hello WhatsApp",
                from = "11999887766",
                to = "11988776655",
                templateName = "order_confirmation",
            )

        val result = request.toCommand()

        result.onRight { command -> assertEquals(NotificationType.WHATSAPP, command.type) }
        assert(result.isRight())
    }

    @Test
    fun `toCommand should map templateName correctly`() {
        val request =
            CreateWhatsAppNotificationRequest(
                content = "Hello WhatsApp",
                from = "11999887766",
                to = "11988776655",
                templateName = "promo_template",
            )

        val result = request.toCommand()

        result.onRight { command -> assertEquals("promo_template", command.templateName) }
        assert(result.isRight())
    }

    @Test
    fun `toCommand should produce a Right with correct command type`() {
        val request =
            CreateWhatsAppNotificationRequest(
                content = "Hello WhatsApp",
                from = "11999887766",
                to = "11988776655",
                templateName = "order_confirmation",
            )

        val result = request.toCommand()

        result.onRight { command -> assert(command is CreateNotificationCommand.WhatsApp) }
        assert(result.isRight())
    }
}
