package br.com.olympus.hermes.shared.infrastructure.providers

import br.com.olympus.hermes.shared.domain.entities.SmsNotification
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
import java.util.Date
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SmsProviderAdapterTest {

    private lateinit var adapter: SmsProviderAdapter

    @BeforeEach
    fun setup() {
        adapter =
                SmsProviderAdapter(
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
    fun `should support SMS notification type`() {
        assertTrue(adapter.supports(NotificationType.SMS))
    }

    @Test
    fun `should send SMS successfully and return receipt`() {
        val notification =
                SmsNotification(
                        content = "Test SMS",
                        payload = emptyMap(),
                        shippingReceipt = null,
                        sentAt = null,
                        deliveryAt = null,
                        seenAt = null,
                        id = EntityId.from("e8febe2c-0e3c-449e-b816-bf7cd0ffed1f").getOrNull()!!,
                        createdAt = Date(),
                        updatedAt = Date(),
                        from = 12345u,
                        to = BrazilianPhone.create("11999999999").getOrNull()!!,
                        isNew = true,
                )

        val messageMock = mockk<Message> { every { sid } returns "SM123" }

        val creatorMock = mockk<MessageCreator> { every { create() } returns messageMock }

        every {
            Message.creator(
                    any<com.twilio.type.PhoneNumber>(),
                    any<com.twilio.type.PhoneNumber>(),
                    "Test SMS"
            )
        } returns creatorMock

        val result = adapter.send(notification)

        assertTrue(result.isRight())
        result.onRight { receipt ->
            assertEquals("SM123", receipt.receiptId)
            assertEquals("twilio", receipt.provider)
        }
    }

    @Test
    fun `should return DeliveryError when Twilio throws exception`() {
        val notification =
                SmsNotification(
                        content = "Test SMS",
                        payload = emptyMap(),
                        shippingReceipt = null,
                        sentAt = null,
                        deliveryAt = null,
                        seenAt = null,
                        id = EntityId.from("e8febe2c-0e3c-449e-b816-bf7cd0ffed1f").getOrNull()!!,
                        createdAt = Date(),
                        updatedAt = Date(),
                        from = 12345u,
                        to = BrazilianPhone.create("11999999999").getOrNull()!!,
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
                    any<String>()
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
