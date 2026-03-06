package br.com.olympus.hermes.notification.application.ports

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.ProviderAdapterNotFoundError
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance

/**
 * Registry that discovers all [NotificationProviderAdapter] CDI beans and routes delivery requests
 * to the correct adapter based on [NotificationType]. New adapters are picked up automatically via
 * CDI [Instance] injection — no manual registration required.
 *
 * @property adapters All [NotificationProviderAdapter] beans available in the CDI context.
 */
@ApplicationScoped
class ProviderAdapterRegistry(
    private val adapters: Instance<NotificationProviderAdapter>,
) {
    /**
     * Resolves the [NotificationProviderAdapter] that supports the given [type].
     *
     * @param type The notification channel type to route.
     * @return Either a [ProviderAdapterNotFoundError] if no adapter supports the type, or the
     * matching [NotificationProviderAdapter].
     */
    fun getAdapter(type: NotificationType): Either<BaseError, NotificationProviderAdapter> {
        val adapter = adapters.firstOrNull { it.supports(type) }
        return adapter?.right() ?: ProviderAdapterNotFoundError(type).left()
    }
}
