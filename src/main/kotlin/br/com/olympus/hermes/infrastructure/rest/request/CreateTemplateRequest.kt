package br.com.olympus.hermes.infrastructure.rest.request

import br.com.olympus.hermes.core.application.commands.CreateTemplateCommand

data class CreateTemplateRequest(
    val name: String,
    val channel: String,
    val subject: String?,
    val body: String,
    val description: String?,
) {
    fun toCommand(): CreateTemplateCommand =
        CreateTemplateCommand(
            name = name,
            channel = channel,
            subject = subject,
            body = body,
            description = description,
        )
}
