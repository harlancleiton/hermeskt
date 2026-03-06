package br.com.olympus.hermes.shared.infrastructure.providers

import arrow.core.Either
import br.com.olympus.hermes.shared.application.ports.NotificationProviderAdapter
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.entities.PushNotification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.DeliveryError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.valueobjects.ProviderReceipt
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.io.FileInputStream
import com.google.firebase.messaging.Notification as FcmNotification

@ApplicationScoped
class PushProviderAdapter(
    @ConfigProperty(name = "hermes.provider.push.firebase-credentials-path", defaultValue = "")
    private val credentialsPath: String,
) : NotificationProviderAdapter {
    private var initialized = false

    override fun supports(type: NotificationType): Boolean = type == NotificationType.PUSH

    override fun send(notification: Notification): Either<BaseError, ProviderReceipt> {
        val pushNotification = notification as PushNotification

        return Either
            .catch {
                initializeFirebaseOnce()

                val fcmNotification =
                    FcmNotification
                        .builder()
                        .setTitle(pushNotification.title)
                        .setBody(pushNotification.content)
                        .build()

                val messageBuilder =
                    Message
                        .builder()
                        .setToken(pushNotification.deviceToken.value)
                        .setNotification(fcmNotification)

                // Add custom data payload if not empty
                if (pushNotification.data.isNotEmpty()) {
                    messageBuilder.putAllData(pushNotification.data)
                }

                val message = messageBuilder.build()
                val messageId = FirebaseMessaging.getInstance().send(message)

                ProviderReceipt(
                    receiptId = messageId,
                    provider = "firebase",
                )
            }.mapLeft { exception ->
                DeliveryError(
                    reason =
                        exception.message
                            ?: "Failed to send push notification via Firebase",
                    cause = exception,
                )
            }
    }

    private fun initializeFirebaseOnce() {
        if (!initialized) {
            if (FirebaseApp.getApps().isEmpty()) {
                val optionsBuilder = FirebaseOptions.builder()

                if (credentialsPath.isNotBlank()) {
                    val serviceAccount = FileInputStream(credentialsPath)
                    optionsBuilder.setCredentials(GoogleCredentials.fromStream(serviceAccount))
                } else {
                    // Warning: Without credentials file, this assumes the environment provides them
                    // e.g., GOOGLE_APPLICATION_CREDENTIALS or running in GCP environment.
                    optionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault())
                }

                FirebaseApp.initializeApp(optionsBuilder.build())
            }
            initialized = true
        }
    }
}
