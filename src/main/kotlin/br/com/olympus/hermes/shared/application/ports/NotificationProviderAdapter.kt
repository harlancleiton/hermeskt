package br.com.olympus.hermes.shared.application.ports

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.valueobjects.ProviderReceipt

/**
 * Output port interface for sending a notification through an external provider. Each channel
 * (Email, SMS, Push, WhatsApp) must provide exactly one implementation annotated as a CDI bean so
 * that [ProviderAdapterRegistry] can discover and route to it automatically.
 */
interface NotificationProviderAdapter {
    /**
     * Sends the notification via the external provider.
     *
     * @param notification The notification aggregate to deliver.
     * @return Either a [BaseError] on failure, or a [ProviderReceipt] containing the
     * provider-specific delivery identifier on success.
     */
    fun send(notification: Notification): Either<BaseError, ProviderReceipt>

    /**
     * Returns `true` if this adapter handles the given [type].
     *
     * @param type The notification channel type to check.
     */
    fun supports(type: NotificationType): Boolean
}
