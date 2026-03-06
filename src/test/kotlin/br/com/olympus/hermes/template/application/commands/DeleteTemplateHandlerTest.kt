package br.com.olympus.hermes.template.application.commands

import arrow.core.right
import br.com.olympus.hermes.shared.domain.exceptions.TemplateNotFoundError
import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeleteTemplateHandlerTest {
    private lateinit var repository: TemplateRepository
    private lateinit var handler: DeleteTemplateHandler

    @BeforeEach
    fun setUp() {
        repository = mockk()
        handler = DeleteTemplateHandler(repository)
    }

    @Test
    fun `should delete template successfully`() {
        every { repository.existsByNameAndChannel(any(), any()) } returns true.right()
        every { repository.deleteByNameAndChannel(any(), any()) } returns true.right()

        val command = DeleteTemplateCommand(name = "welcome-email", channel = "EMAIL")

        val result = handler.handle(command)

        assert(result.isRight())
        verify { repository.deleteByNameAndChannel(any(), any()) }
    }

    @Test
    fun `should return TemplateNotFoundError when template does not exist`() {
        every { repository.existsByNameAndChannel(any(), any()) } returns false.right()
        every { repository.deleteByNameAndChannel(any(), any()) } returns false.right()

        val command = DeleteTemplateCommand(name = "nonexistent", channel = "EMAIL")

        val result = handler.handle(command)

        assert(result.isLeft())
        result.onLeft { assertTrue(it is TemplateNotFoundError) }
    }
}
