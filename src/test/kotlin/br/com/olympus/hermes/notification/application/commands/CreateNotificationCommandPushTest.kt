package br.com.olympus.hermes.notification.application.commands

import br.com.olympus.hermes.notification.domain.factories.CreateNotificationInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateNotificationCommandPushTest {
    @Test
    fun `toInput should map all fields correctly`() {
        val command =
            CreateNotificationCommand.Push(
                id = "id123",
                content = "body",
                payload = mapOf("key" to "value"),
                deviceToken = "tokenxyz",
                title = "title",
                data = mapOf("dataKey" to "dataValue"),
            )

        val input = command.toInput()
        assertTrue(input is CreateNotificationInput.Push)
        input as CreateNotificationInput.Push
        assertEquals("id123", input.id)
        assertEquals("body", input.content)
        assertEquals("value", input.payload["key"])
        assertEquals("tokenxyz", input.deviceToken)
        assertEquals("title", input.title)
        assertEquals("dataValue", input.data["dataKey"])
    }
}
