package br.com.olympus.hermes.shared.infrastructure.providers

import arrow.core.Either
import br.com.olympus.hermes.shared.application.ports.NotificationProviderAdapter
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.entities.WhatsAppNotification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.DeliveryError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.valueobjects.ProviderReceipt
import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class WhatsAppProviderAdapter(
    @ConfigProperty(name = "hermes.provider.whatsapp.twilio-account-sid")
    private val accountSid: String,
    @ConfigProperty(name = "hermes.provider.whatsapp.twilio-auth-token")
    private val authToken: String,
) : NotificationProviderAdapter {
    private var initialized = false

    override fun supports(type: NotificationType): Boolean = type == NotificationType.WHATSAPP

    override fun send(notification: Notification): Either<BaseError, ProviderReceipt> {
        val whatsAppNotification = notification as WhatsAppNotification

        return Either
            .catch {
                if (!initialized) {
                    Twilio.init(accountSid, authToken)
                    initialized = true
                }

                // Twilio WhatsApp API requires the "whatsapp:" prefix
                val to = PhoneNumber("whatsapp:${whatsAppNotification.to.value}")
                val from = PhoneNumber("whatsapp:${whatsAppNotification.from.value}")

                val message =
                    Message
                        .creator(
                            to,
                            from,
                            whatsAppNotification.content,
                        ).create()

                ProviderReceipt(
                    receiptId = message.sid,
                    provider = "twilio-whatsapp",
                )
            }.mapLeft { exception ->
                DeliveryError(
                    reason =
                        exception.message
                            ?: "Failed to send WhatsApp message via Twilio",
                    cause = exception,
                )
            }
    }
}
