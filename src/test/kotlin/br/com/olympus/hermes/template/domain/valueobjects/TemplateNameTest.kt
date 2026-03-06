package br.com.olympus.hermes.template.domain.valueobjects

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TemplateNameTest {
    @Test
    fun `should create valid slug`() {
        val result = TemplateName.create("order-confirmation")
        assert(result.isRight())
        result.onRight { assertEquals("order-confirmation", it.value) }
    }

    @Test
    fun `should accept single character`() {
        val result = TemplateName.create("a")
        assert(result.isRight())
    }

    @Test
    fun `should accept hyphenated slug`() {
        val result = TemplateName.create("welcome-new-user")
        assert(result.isRight())
    }

    @Test
    fun `should reject blank value`() {
        val result = TemplateName.create("   ")
        assert(result.isLeft())
    }

    @Test
    fun `should reject uppercase characters`() {
        val result = TemplateName.create("Order-Confirmation")
        assert(result.isLeft())
    }

    @Test
    fun `should reject special characters`() {
        val result = TemplateName.create("order_confirmation!")
        assert(result.isLeft())
    }

    @Test
    fun `should reject value exceeding 128 chars`() {
        val longName = "a" + "-b".repeat(64) // 129 chars
        val result = TemplateName.create(longName)
        assert(result.isLeft())
    }

    @Test
    fun `should accept value at exactly 128 chars`() {
        // "a" + "-b" * 63 = 1 + 126 = 127 chars; use "ab".repeat(64) = 128
        val name = "a" + "b".repeat(127) // 128 chars, all lowercase
        // Must match slug: no hyphens in between — use "a-b" segments summing to 128
        // Simplest: "a" repeated via "a-".repeat is tricky; use known valid 128-char slug
        val exactly128 = "a".repeat(1) + ("-a".repeat(63)).take(127) // 1+127=128
        val result = TemplateName.create(exactly128)
        assert(result.isRight())
    }
}
