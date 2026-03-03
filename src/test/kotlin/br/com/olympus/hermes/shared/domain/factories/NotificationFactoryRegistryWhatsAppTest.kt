package br.com.olympus.hermes.shared.domain.factories

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.entities.WhatsAppNotification
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NotificationFactoryRegistryWhatsAppTest {
    private lateinit var registry: NotificationFactoryRegistry

    @BeforeEach
    fun setUp() {
        registry = NotificationFactoryRegistry()
    }

    @Test
    fun `getFactory_whatsApp_returnsRight`() {
        val result = registry.getFactory<WhatsAppNotification>(NotificationType.WHATSAPP)

        assertTrue(result.isRight())
        val factory = (result as Either.Right).value
        assertNotNull(factory)
        assertTrue(factory is WhatsAppNotificationFactory)
    }

    @Test
    fun `getFactory_whatsApp_canCreateValidNotification`() {
        val result = registry.getFactory<WhatsAppNotification>(NotificationType.WHATSAPP)
        assertTrue(result.isRight())
        val factory = (result as Either.Right).value

        val input =
            CreateNotificationInput.WhatsApp(
                content = "Hello, world!",
                from = "11987654321",
                to = "11912345678",
                templateName = "hello_world",
            )

        val notification = factory.create(input)

        assertTrue(notification.isRight())
        val whatsApp = (notification as Either.Right).value
        assertEquals("Hello, world!", whatsApp.content)
        assertEquals(BrazilianPhone.create("11987654321").getOrNull(), whatsApp.from)
        assertEquals(BrazilianPhone.create("11912345678").getOrNull(), whatsApp.to)
        assertEquals("hello_world", whatsApp.templateName)
    }
}
