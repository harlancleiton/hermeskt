package br.com.olympus.hermes.template.infrastructure.rest.request

import br.com.olympus.hermes.template.application.commands.UpdateTemplateCommand

data class UpdateTemplateRequest(
    val channel: String,
    val subject: String?,
    val body: String?,
    val description: String?,
) {
    fun toCommand(name: String): UpdateTemplateCommand =
        UpdateTemplateCommand(
            name = name,
            channel = channel,
            subject = subject,
            body = body,
            description = description,
        )
}
