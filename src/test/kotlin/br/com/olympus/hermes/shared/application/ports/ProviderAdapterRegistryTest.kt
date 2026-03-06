package br.com.olympus.hermes.shared.application.ports

import br.com.olympus.hermes.shared.domain.exceptions.ProviderAdapterNotFoundError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProviderAdapterRegistryTest {
    private fun makeAdapter(type: NotificationType): NotificationProviderAdapter =
        mockk {
            every { supports(type) } returns true
            every { supports(neq(type)) } returns false
        }

    private fun makeRegistry(vararg adapters: NotificationProviderAdapter): ProviderAdapterRegistry {
        val instance: Instance<NotificationProviderAdapter> =
            mockk {
                every { iterator() } returns adapters.toList().iterator()
                every { firstOrNull(any()) } answers
                    {
                        val predicate = firstArg<(NotificationProviderAdapter) -> Boolean>()
                        adapters.firstOrNull { predicate(it) }
                    }
            }
        return ProviderAdapterRegistry(instance)
    }

    @Test
    fun `should resolve adapter for EMAIL type`() {
        val emailAdapter = makeAdapter(NotificationType.EMAIL)
        val registry = makeRegistry(emailAdapter)

        val result = registry.getAdapter(NotificationType.EMAIL)

        assertTrue(result.isRight())
        result.onRight { assertTrue(it.supports(NotificationType.EMAIL)) }
    }

    @Test
    fun `should resolve adapter for SMS type`() {
        val smsAdapter = makeAdapter(NotificationType.SMS)
        val registry = makeRegistry(smsAdapter)

        val result = registry.getAdapter(NotificationType.SMS)

        assertTrue(result.isRight())
    }

    @Test
    fun `should resolve adapter for PUSH type`() {
        val pushAdapter = makeAdapter(NotificationType.PUSH)
        val registry = makeRegistry(pushAdapter)

        val result = registry.getAdapter(NotificationType.PUSH)

        assertTrue(result.isRight())
    }

    @Test
    fun `should resolve adapter for WHATSAPP type`() {
        val waAdapter = makeAdapter(NotificationType.WHATSAPP)
        val registry = makeRegistry(waAdapter)

        val result = registry.getAdapter(NotificationType.WHATSAPP)

        assertTrue(result.isRight())
    }

    @Test
    fun `should return ProviderAdapterNotFoundError for unregistered type`() {
        val registry = makeRegistry() // no adapters

        val result = registry.getAdapter(NotificationType.EMAIL)

        assertTrue(result.isLeft())
        result.onLeft { assertInstanceOf(ProviderAdapterNotFoundError::class.java, it) }
    }
}

private fun <T> neq(value: T): T = io.mockk.match { it != value }
