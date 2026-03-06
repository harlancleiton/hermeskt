package br.com.olympus.hermes.shared.infrastructure.providers

import br.com.olympus.hermes.shared.domain.entities.WhatsAppNotification
import br.com.olympus.hermes.shared.domain.exceptions.DeliveryError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.rest.api.v2010.account.MessageCreator
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

class WhatsAppProviderAdapterTest {
    private lateinit var adapter: WhatsAppProviderAdapter

    @BeforeEach
    fun setup() {
        adapter =
            WhatsAppProviderAdapter(
                accountSid = "test-sid",
                authToken = "test-token",
            )
        mockkStatic(Twilio::class)
        every { Twilio.init(any<String>(), any<String>()) } returns Unit
        mockkStatic(Message::class)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(Twilio::class)
        unmockkStatic(Message::class)
    }

    @Test
    fun `should support WHATSAPP notification type`() {
        assertTrue(adapter.supports(NotificationType.WHATSAPP))
    }

    @Test
    fun `should send WhatsApp message successfully and return receipt`() {
        val notification =
            WhatsAppNotification(
                content = "Test WhatsApp Message",
                payload = emptyMap(),
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                id = EntityId.generate(),
                createdAt = Date(),
                updatedAt = Date(),
                from = BrazilianPhone.create("11999999999").getOrNull()!!,
                to = BrazilianPhone.create("11988888888").getOrNull()!!,
                templateName = "test_template",
                isNew = true,
            )

        val messageMock = mockk<Message> { every { sid } returns "SM123" }

        val creatorMock = mockk<MessageCreator> { every { create() } returns messageMock }

        every {
            Message.creator(
                any<com.twilio.type.PhoneNumber>(),
                any<com.twilio.type.PhoneNumber>(),
                "Test WhatsApp Message",
            )
        } returns creatorMock

        val result = adapter.send(notification)

        assertTrue(result.isRight())
        result.onRight { receipt ->
            assertEquals("SM123", receipt.receiptId)
            assertEquals("twilio-whatsapp", receipt.provider)
        }
    }

    @Test
    fun `should return DeliveryError when Twilio throws exception`() {
        val notification =
            WhatsAppNotification(
                content = "Test WhatsApp Message",
                payload = emptyMap(),
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                id = EntityId.generate(),
                createdAt = Date(),
                updatedAt = Date(),
                from = BrazilianPhone.create("11999999999").getOrNull()!!,
                to = BrazilianPhone.create("11988888888").getOrNull()!!,
                templateName = "test_template",
                isNew = true,
            )

        val creatorMock =
            mockk<MessageCreator> {
                every { create() } throws RuntimeException("Twilio offline")
            }

        every {
            Message.creator(
                any<com.twilio.type.PhoneNumber>(),
                any<com.twilio.type.PhoneNumber>(),
                any<String>(),
            )
        } returns creatorMock

        val result = adapter.send(notification)

        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertTrue(error is DeliveryError)
            assertEquals("Notification delivery failed: Twilio offline", error.message)
        }
    }
}
