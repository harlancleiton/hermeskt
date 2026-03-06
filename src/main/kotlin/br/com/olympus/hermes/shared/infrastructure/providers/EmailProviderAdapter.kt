package br.com.olympus.hermes.shared.infrastructure.providers

import arrow.core.Either
import br.com.olympus.hermes.shared.application.ports.NotificationProviderAdapter
import br.com.olympus.hermes.shared.domain.entities.EmailNotification
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.DeliveryError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.valueobjects.ProviderReceipt
import jakarta.enterprise.context.ApplicationScoped
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Properties

@ApplicationScoped
class EmailProviderAdapter(
    @ConfigProperty(name = "hermes.provider.email.smtp-host") private val host: String,
    @ConfigProperty(name = "hermes.provider.email.smtp-port") private val port: Int,
    @ConfigProperty(name = "hermes.provider.email.smtp-username") private val username: String,
    @ConfigProperty(name = "hermes.provider.email.smtp-password") private val password: String,
    @ConfigProperty(name = "hermes.provider.email.smtp-starttls") private val startTls: Boolean,
    @ConfigProperty(name = "hermes.provider.email.timeout-ms") private val timeoutMs: Int,
) : NotificationProviderAdapter {
    override fun supports(type: NotificationType): Boolean = type == NotificationType.EMAIL

    override fun send(notification: Notification): Either<BaseError, ProviderReceipt> {
        val emailNotification = notification as EmailNotification

        return Either
            .catch {
                val props =
                    Properties().apply {
                        put("mail.smtp.auth", username.isNotBlank())
                        put("mail.smtp.starttls.enable", startTls)
                        put("mail.smtp.host", host)
                        put("mail.smtp.port", port)
                        put("mail.smtp.connectiontimeout", timeoutMs)
                        put("mail.smtp.timeout", timeoutMs)
                        put("mail.smtp.writetimeout", timeoutMs)
                    }

                val session =
                    if (username.isNotBlank()) {
                        Session.getInstance(
                            props,
                            object : Authenticator() {
                                override fun getPasswordAuthentication() = PasswordAuthentication(username, password)
                            },
                        )
                    } else {
                        Session.getInstance(props)
                    }

                val message =
                    MimeMessage(session).apply {
                        setFrom(InternetAddress(emailNotification.from.value))
                        setRecipients(
                            Message.RecipientType.TO,
                            InternetAddress.parse(emailNotification.to.value),
                        )
                        subject = emailNotification.subject.subject
                        setText(emailNotification.content)
                    }

                Transport.send(message)

                // Transport.send doesn't directly return a message ID, but MimeMessage might generate
                // one
                val messageId = message.messageID ?: "UNKNOWN"

                ProviderReceipt(
                    receiptId = messageId,
                    provider = "smtp",
                )
            }.mapLeft { exception ->
                DeliveryError(
                    reason = exception.message ?: "Failed to send email via SMTP",
                    cause = exception,
                )
            }
    }
}
