package br.com.olympus.hermes.infrastructure.rest.response

import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Date

class NotificationResponsePushTest {
    @Test
    fun `from should return PUSH type for PushNotification`() {
        val view = NotificationView()
        view.id = "id123"
        view.type = "PUSH"
        view.createdAt = Date()
        view.updatedAt = Date()

        val response = NotificationResponse.from(view)
        assertEquals("id123", response.id)
        assertEquals(NotificationType.PUSH, response.type)
        assertEquals(view.createdAt, response.createdAt)
        assertEquals(view.updatedAt, response.updatedAt)
    }
}
