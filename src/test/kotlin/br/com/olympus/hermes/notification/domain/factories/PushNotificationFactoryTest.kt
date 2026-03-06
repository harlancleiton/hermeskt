package br.com.olympus.hermes.notification.domain.factories

import br.com.olympus.hermes.notification.domain.events.*
import br.com.olympus.hermes.notification.domain.valueobjects.DeviceToken
import br.com.olympus.hermes.shared.domain.events.EventWrapper
import br.com.olympus.hermes.shared.domain.exceptions.EmptyContentError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidDeviceTokenError
import br.com.olympus.hermes.shared.domain.exceptions.InvalidEventHistoryError
import br.com.olympus.hermes.shared.domain.exceptions.MissingCreationEventError
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID

class PushNotificationFactoryTest {
    private val factory = PushNotificationFactory()

    @Test
    fun `create should return PushNotification for valid input`() {
        val input =
            CreateNotificationInput.Push(
                id = UUID.randomUUID().toString(),
                content = "Valid Content",
                payload = emptyMap(),
                deviceToken = "device-token-123",
                title = "Valid Title",
                data = mapOf("key" to "val"),
            )

        val result = factory.create(input)
        assertTrue(result.isRight())
        val notification = result.getOrNull()!!
        assertEquals(input.id, notification.id.value.toString())
        assertEquals("Valid Content", notification.content)
        assertEquals("device-token-123", notification.deviceToken.value)
        assertEquals("Valid Title", notification.title)
        assertEquals("val", notification.data["key"])

        // Ensure event was created
        assertEquals(1, notification.uncommittedChanges.size)
    }

    @Test
    fun `create should accumulate errors for all invalid fields`() {
        val input =
            CreateNotificationInput.Push(
                id = UUID.randomUUID().toString(),
                content = "   ",
                payload = emptyMap(),
                deviceToken = "", // Invalid
                title = "   ", // Invalid
                data = emptyMap(),
            )

        val result = factory.create(input)
        assertTrue(result.isLeft())
        val validationErrors = result.leftOrNull()!!
        assertEquals(3, validationErrors.errors.size)

        assertTrue(validationErrors.errors.any { it is EmptyContentError && it.field == "content" })
        assertTrue(validationErrors.errors.any { it is InvalidDeviceTokenError })
        assertTrue(validationErrors.errors.any { it is EmptyContentError && it.field == "title" })
    }

    @Test
    fun `create should return InvalidNotificationInputError for wrong input type`() {
        val input =
            CreateNotificationInput.Email(
                id = UUID.randomUUID().toString(),
                content = "body",
                from = "a@a.com",
                to = "b@b.com",
                subject = "sub",
            )
        val result = factory.create(input)
        assertTrue(result.isLeft())
        val errors = result.leftOrNull()!!
        assertEquals(
            "Push",
            (
                errors.errors.first() as
                    br.com.olympus.hermes.shared.domain.exceptions.InvalidNotificationInputError
            ).expected,
        )
    }

    @Test
    fun `reconstitute should rebuild aggregate from event history`() {
        val event =
            PushNotificationCreatedEvent(
                aggregateId = UUID.randomUUID().toString(),
                content = "Reconstituted Content",
                payload = emptyMap(),
                deviceToken = DeviceToken.create("token123").getOrNull()!!,
                title = "Reconstituted Title",
                data = emptyMap(),
            )
        val envelope =
            EventWrapper(
                aggregateId = EntityId.from(event.aggregateId).getOrNull()!!,
                aggregateType = "PushNotification",
                aggregateVersion = 1,
                eventType = "PushNotificationCreatedEvent",
                occurredAt = Date(),
                payload = event,
            )

        val result = factory.reconstitute(listOf(envelope))
        assertTrue(result.isRight())
        val notification = result.getOrNull()!!

        assertEquals(event.aggregateId, notification.id.value.toString())
        assertEquals("Reconstituted Content", notification.content)
        assertEquals("token123", notification.deviceToken.value)
        assertEquals("Reconstituted Title", notification.title)
        assertTrue(notification.uncommittedChanges.isEmpty()) // no new events
    }

    @Test
    fun `reconstitute should return InvalidEventHistoryError for empty events`() {
        val result = factory.reconstitute(emptyList())
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is InvalidEventHistoryError)
    }

    @Test
    fun `reconstitute should return MissingCreationEventError when creation event missing`() {
        val nonCreationEvent =
            br.com.olympus.hermes.notification.domain.events
                .NotificationDeliveredEvent(Date())
        val envelope =
            EventWrapper(
                aggregateId = EntityId.generate(),
                aggregateType = "PushNotification",
                aggregateVersion = 1,
                eventType = "NotificationDeliveredEvent",
                occurredAt = Date(),
                payload = nonCreationEvent,
            )
        val result = factory.reconstitute(listOf(envelope))
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is MissingCreationEventError)
    }
}
