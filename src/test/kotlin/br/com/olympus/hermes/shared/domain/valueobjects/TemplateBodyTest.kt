package br.com.olympus.hermes.shared.domain.valueobjects

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TemplateBodyTest {
    @Test
    fun `should create valid body`() {
        val result = TemplateBody.create("Hello {{name}}, welcome!")
        assert(result.isRight())
        result.onRight { assertEquals("Hello {{name}}, welcome!", it.value) }
    }

    @Test
    fun `should reject blank body`() {
        val result = TemplateBody.create("   ")
        assert(result.isLeft())
    }

    @Test
    fun `should reject body exceeding 64 KB`() {
        val tooLong = "a".repeat(65537)
        val result = TemplateBody.create(tooLong)
        assert(result.isLeft())
    }

    @Test
    fun `should accept body at exactly 64 KB`() {
        val exactly64KB = "a".repeat(65536)
        val result = TemplateBody.create(exactly64KB)
        assert(result.isRight())
    }
}
