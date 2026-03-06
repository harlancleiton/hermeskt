package br.com.olympus.hermes.shared.infrastructure.providers

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.entities.PushNotification
import br.com.olympus.hermes.shared.domain.exceptions.DeliveryError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.valueobjects.DeviceToken
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

class PushProviderAdapterTest {
    private lateinit var adapter: PushProviderAdapter

    @BeforeEach
    fun setUp() {
        adapter = PushProviderAdapter(credentialsPath = "")

        mockkStatic(FirebaseApp::class)
        mockkStatic(FirebaseMessaging::class)
        mockkStatic(GoogleCredentials::class)
        every { GoogleCredentials.getApplicationDefault() } returns mockk()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should support PUSH notification type`() {
        assertTrue(adapter.supports(NotificationType.PUSH))
    }

    @Test
    fun `should send push notification successfully`() {
        // Given
        val mockFirebaseApp = mockk<FirebaseApp>()
        every { FirebaseApp.getApps() } returns listOf(mockFirebaseApp)

        val mockFirebaseMessaging = mockk<FirebaseMessaging>()
        every { FirebaseMessaging.getInstance() } returns mockFirebaseMessaging
        every { mockFirebaseMessaging.send(any<Message>()) } returns
            "projects/my-project/messages/12345"

        val pushNotification =
            PushNotification(
                id = EntityId.generate(),
                content = "Update available",
                payload = emptyMap(),
                title = "New Update",
                deviceToken = DeviceToken.create("token123").getOrNull()!!,
                data = mapOf("key" to "value"),
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                createdAt = Date(),
                updatedAt = Date(),
                isNew = true,
            )

        // When
        val result = adapter.send(pushNotification)

        // Then
        assertTrue(result is Either.Right)
        result.onRight { receipt ->
            assertEquals("firebase", receipt.provider)
            assertEquals("projects/my-project/messages/12345", receipt.receiptId)
        }

        verify(exactly = 1) { mockFirebaseMessaging.send(any()) }
    }

    @Test
    fun `should map exceptions to DeliveryError`() {
        // Given
        val mockFirebaseApp = mockk<FirebaseApp>()
        every { FirebaseApp.getApps() } returns listOf(mockFirebaseApp)

        val mockFirebaseMessaging = mockk<FirebaseMessaging>()
        every { FirebaseMessaging.getInstance() } returns mockFirebaseMessaging
        every { mockFirebaseMessaging.send(any<Message>()) } throws
            RuntimeException("Firebase error")

        val pushNotification =
            PushNotification(
                id = EntityId.generate(),
                content = "Update available",
                payload = emptyMap(),
                title = "New Update",
                deviceToken = DeviceToken.create("token123").getOrNull()!!,
                data = emptyMap(), // Test empty data branch
                shippingReceipt = null,
                sentAt = null,
                deliveryAt = null,
                seenAt = null,
                createdAt = Date(),
                updatedAt = Date(),
                isNew = true,
            )

        // When
        val result = adapter.send(pushNotification)

        // Then
        assertTrue(result is Either.Left)
        result.onLeft { error ->
            assertTrue(error is DeliveryError)
            assertEquals("Firebase error", (error as DeliveryError).reason)
        }
    }
}
