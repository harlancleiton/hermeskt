package br.com.olympus.hermes.shared.domain.factories

import br.com.olympus.hermes.shared.domain.entities.Notification

class NotificationFactoryRegistry {
    private val factories = mutableMapOf<NotificationType, NotificationFactory<out Notification>>()

    fun <T : Notification> register(type: NotificationType, factory: NotificationFactory<T>) {
        if (factories.containsKey(type)) {
            throw IllegalStateException("Factory for type $type is already registered")
        }
        factories[type] = factory
    }

    fun <T : Notification> registerOrReplace(type: NotificationType, factory: NotificationFactory<T>) {
        factories[type] = factory
    }

    fun unregister(type: NotificationType): Boolean {
        return factories.remove(type) != null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Notification> getFactory(type: NotificationType): NotificationFactory<T> {
        return factories[type] as? NotificationFactory<T>
            ?: throw NotificationFactoryNotFoundException(type)
    }

    fun hasFactory(type: NotificationType): Boolean {
        return factories.containsKey(type)
    }

    fun getSupportedTypes(): Set<NotificationType> {
        return factories.keys.toSet()
    }

    fun clear() {
        factories.clear()
    }

    class NotificationFactoryNotFoundException(type: NotificationType) :
        RuntimeException("No factory registered for notification type: $type")
}

