package br.com.olympus.hermes.shared.infrastructure.providers

import arrow.core.Either
import br.com.olympus.hermes.shared.application.ports.NotificationProviderAdapter
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.entities.SmsNotification
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
class SmsProviderAdapter(
    @ConfigProperty(name = "hermes.provider.sms.twilio-account-sid")
    private val accountSid: String,
    @ConfigProperty(name = "hermes.provider.sms.twilio-auth-token")
    private val authToken: String,
) : NotificationProviderAdapter {
    private var initialized = false

    override fun supports(type: NotificationType): Boolean = type == NotificationType.SMS

    override fun send(notification: Notification): Either<BaseError, ProviderReceipt> {
        val smsNotification = notification as SmsNotification

        return Either
            .catch {
                if (!initialized) {
                    Twilio.init(accountSid, authToken)
                    initialized = true
                }

                val to = PhoneNumber(smsNotification.to.value)
                val from = PhoneNumber(smsNotification.from.toString())

                val message =
                    Message
                        .creator(
                            to,
                            from,
                            smsNotification.content,
                        ).create()

                ProviderReceipt(
                    receiptId = message.sid,
                    provider = "twilio",
                )
            }.mapLeft { exception ->
                DeliveryError(
                    reason = exception.message ?: "Failed to send SMS via Twilio",
                    cause = exception,
                )
            }
    }
}
