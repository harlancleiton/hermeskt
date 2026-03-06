package br.com.olympus.hermes.notification.domain.valueobjects

import br.com.olympus.hermes.shared.domain.exceptions.InvalidDeviceTokenError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceTokenTest {
    @Test
    fun `should create valid DeviceToken from non-blank string`() {
        val result = DeviceToken.create("valid_token")
        assertTrue(result.isRight())
        assertEquals("valid_token", result.getOrNull()?.value)
    }

    @Test
    fun `should return InvalidDeviceTokenError for blank string`() {
        val result = DeviceToken.create("")
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is InvalidDeviceTokenError)
    }

    @Test
    fun `should return InvalidDeviceTokenError for whitespace-only string`() {
        val result = DeviceToken.create("   ")
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is InvalidDeviceTokenError)
    }

    @Test
    fun `should return InvalidDeviceTokenError for string exceeding 4096 chars`() {
        val longToken = "a".repeat(4097)
        val result = DeviceToken.create(longToken)
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is InvalidDeviceTokenError)
    }

    @Test
    fun `should create DeviceToken with exactly 4096 chars`() {
        val maxToken = "a".repeat(4096)
        val result = DeviceToken.create(maxToken)
        assertTrue(result.isRight())
        assertEquals(maxToken, result.getOrNull()?.value)
    }

    @Test
    fun `should trim whitespace from valid token`() {
        val result = DeviceToken.create("  valid_token  ")
        assertTrue(result.isRight())
        assertEquals("valid_token", result.getOrNull()?.value)
    }
}
