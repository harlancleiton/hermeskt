package br.com.olympus.hermes.shared.domain.factories

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.exceptions.EmptyContentError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidNotificationInputError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidPhoneError
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WhatsAppNotificationFactoryTest {

        private lateinit var factory: WhatsAppNotificationFactory

        private val validFrom = "11987654321"
        private val validTo = "11912345678"
        private val validContent = "Hello, this is a test message"
        private val validTemplateName = "hello_world"

        @BeforeEach
        fun setUp() {
                factory = WhatsAppNotificationFactory()
        }

        @Test
        fun `create_withValidInput_returnsRight`() {
                val input =
                        CreateNotificationInput.WhatsApp(
                                content = validContent,
                                from = validFrom,
                                to = validTo,
                                templateName = validTemplateName
                        )

                val result = factory.create(input)

                assertTrue(result.isRight())
                val notification = (result as Either.Right).value
                assertEquals(validContent, notification.content)
                assertEquals(validFrom, notification.from.value)
                assertEquals(validTo, notification.to.value)
                assertEquals(validTemplateName, notification.templateName)
        }

        @Test
        fun `create_withInvalidFromPhone_returnsLeftWithInvalidPhoneError`() {
                val input =
                        CreateNotificationInput.WhatsApp(
                                content = validContent,
                                from = "invalid",
                                to = validTo,
                                templateName = validTemplateName
                        )

                val result = factory.create(input)

                assertTrue(result.isLeft())
                val errors = (result as Either.Left).value
                assertTrue(errors.errors.any { it is InvalidPhoneError })
        }

        @Test
        fun `create_withInvalidToPhone_returnsLeftWithInvalidPhoneError`() {
                val input =
                        CreateNotificationInput.WhatsApp(
                                content = validContent,
                                from = validFrom,
                                to = "invalid",
                                templateName = validTemplateName
                        )

                val result = factory.create(input)

                assertTrue(result.isLeft())
                val errors = (result as Either.Left).value
                assertTrue(errors.errors.any { it is InvalidPhoneError })
        }

        @Test
        fun `create_withBlankContent_returnsLeftWithEmptyContentError`() {
                val input =
                        CreateNotificationInput.WhatsApp(
                                content = "   ",
                                from = validFrom,
                                to = validTo,
                                templateName = validTemplateName
                        )

                val result = factory.create(input)

                assertTrue(result.isLeft())
                val errors = (result as Either.Left).value
                assertTrue(errors.errors.any { it is EmptyContentError && it.field == "content" })
        }

        @Test
        fun `create_withBlankTemplateName_returnsLeftWithEmptyContentError`() {
                val input =
                        CreateNotificationInput.WhatsApp(
                                content = validContent,
                                from = validFrom,
                                to = validTo,
                                templateName = "   "
                        )

                val result = factory.create(input)

                assertTrue(result.isLeft())
                val errors = (result as Either.Left).value
                assertTrue(
                        errors.errors.any { it is EmptyContentError && it.field == "templateName" }
                )
        }

        @Test
        fun `create_withMultipleInvalidFields_accumulatesAllErrors`() {
                val input =
                        CreateNotificationInput.WhatsApp(
                                content = "   ",
                                from = "invalid",
                                to = "bad",
                                templateName = "   "
                        )

                val result = factory.create(input)

                assertTrue(result.isLeft())
                val errors = (result as Either.Left).value
                assertTrue(errors.errors.size >= 3)
                assertTrue(errors.errors.any { it is EmptyContentError && it.field == "content" })
                assertTrue(errors.errors.any { it is InvalidPhoneError })
                assertTrue(
                        errors.errors.any { it is EmptyContentError && it.field == "templateName" }
                )
        }

        @Test
        fun `create_withWrongInputType_returnsLeftWithInvalidNotificationInputError`() {
                val input =
                        CreateNotificationInput.Email(
                                content = validContent,
                                from = "sender@example.com",
                                to = "recipient@example.com",
                                subject = "Test"
                        )

                val result = factory.create(input)

                assertTrue(result.isLeft())
                val errors = (result as Either.Left).value
                assertTrue(errors.errors.any { it is InvalidNotificationInputError })
        }

        @Test
        fun `reconstitute_withValidEvents_returnsRight`() {
                val from = BrazilianPhone.create(validFrom).getOrNull()!!
                val to = BrazilianPhone.create(validTo).getOrNull()!!
                val now = Date()
                val aggregateId = EntityId.generate()

                val creationEvent =
                        br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent(
                                id = EntityId.generate(),
                                aggregateId = aggregateId,
                                aggregateVersion = 0,
                                occurredAt = now,
                                content = validContent,
                                payload = emptyMap(),
                                from = from,
                                to = to,
                                templateName = validTemplateName
                        )

                val result = factory.reconstitute(listOf(creationEvent))

                assertTrue(result.isRight())
                val notification = (result as Either.Right).value
                assertEquals(aggregateId, notification.id)
                assertEquals(validContent, notification.content)
                assertEquals(validFrom, notification.from.value)
                assertEquals(validTo, notification.to.value)
                assertEquals(validTemplateName, notification.templateName)
        }

        @Test
        fun `reconstitute_withEmptyEvents_returnsLeftWithInvalidEventHistoryError`() {
                val result = factory.reconstitute(emptyList())

                assertTrue(result.isLeft())
                val error = (result as Either.Left).value
                assertTrue(
                        error is
                                br.com.olympus.hermes.shared.domain.exceptions.InvalidEventHistoryError
                )
        }

        @Test
        fun `reconstitute_withMissingCreationEvent_returnsLeftWithMissingCreationEventError`() {
                val sentEvent =
                        br.com.olympus.hermes.shared.domain.events.NotificationSentEvent(
                                id = EntityId.generate(),
                                aggregateId = EntityId.generate(),
                                aggregateVersion = 1,
                                occurredAt = Date(),
                                shippingReceipt = "receipt-123"
                        )

                val result = factory.reconstitute(listOf(sentEvent))

                assertTrue(result.isLeft())
                val error = (result as Either.Left).value
                assertTrue(
                        error is
                                br.com.olympus.hermes.shared.domain.exceptions.MissingCreationEventError
                )
        }
}
