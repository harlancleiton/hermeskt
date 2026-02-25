package br.com.olympus.hermes.shared.domain.factories

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.FactoryAlreadyRegisteredError
import br.com.olympus.hermes.shared.domain.exceptions.FactoryNotFoundError
import jakarta.enterprise.context.ApplicationScoped

/**
 * Registry for managing notification factories. Provides type-safe factory registration and
 * retrieval using functional error handling. Implements the Factory pattern with a registry to
 * decouple notification creation from specific factory implementations.
 */
@ApplicationScoped
class NotificationFactoryRegistry {
    private val factories = mutableMapOf<NotificationType, NotificationFactory<out Notification>>()

    init {
        register(NotificationType.EMAIL, EmailNotificationFactory())
        register(NotificationType.SMS, SmsNotificationFactory())
    }

    /**
     * Registers a factory for a specific notification type. Returns an error if a factory for the
     * type is already registered.
     *
     * @param type The notification type.
     * @param factory The factory instance to register.
     * @return Either FactoryAlreadyRegisteredError if already registered, or Unit on success.
     */
    fun <T : Notification> register(
            type: NotificationType,
            factory: NotificationFactory<T>
    ): Either<BaseError, Unit> {
        return if (factories.containsKey(type)) {
            FactoryAlreadyRegisteredError(type).left()
        } else {
            factories[type] = factory
            Unit.right()
        }
    }

    /**
     * Registers a factory for a specific notification type, replacing any existing factory. Always
     * succeeds.
     *
     * @param type The notification type.
     * @param factory The factory instance to register.
     */
    fun <T : Notification> registerOrReplace(
            type: NotificationType,
            factory: NotificationFactory<T>
    ) {
        factories[type] = factory
    }

    /**
     * Unregisters the factory for a specific notification type.
     *
     * @param type The notification type.
     * @return true if a factory was removed, false if no factory was registered for the type.
     */
    fun unregister(type: NotificationType): Boolean {
        return factories.remove(type) != null
    }

    /**
     * Retrieves a factory for a specific notification type. Returns an error if no factory is
     * registered for the type.
     *
     * @param type The notification type.
     * @return Either FactoryNotFoundError if not found, or the factory instance.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Notification> getFactory(
            type: NotificationType
    ): Either<BaseError, NotificationFactory<T>> {
        val factory = factories[type] as? NotificationFactory<T>
        return factory?.right() ?: FactoryNotFoundError(type).left()
    }

    /**
     * Checks if a factory is registered for a specific notification type.
     *
     * @param type The notification type.
     * @return true if a factory is registered, false otherwise.
     */
    fun hasFactory(type: NotificationType): Boolean {
        return factories.containsKey(type)
    }

    /**
     * Returns the set of all registered notification types.
     *
     * @return Set of notification types that have registered factories.
     */
    fun getSupportedTypes(): Set<NotificationType> {
        return factories.keys.toSet()
    }

    /**
     * Clears all registered factories. Use with caution - primarily intended for testing purposes.
     */
    fun clear() {
        factories.clear()
    }
}
