package br.com.olympus.hermes.template.infrastructure.rest.response

import br.com.olympus.hermes.template.domain.entities.NotificationTemplate

data class TemplateResponse(
    val name: String,
    val channel: String,
    val subject: String?,
    val body: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(template: NotificationTemplate): TemplateResponse =
            TemplateResponse(
                name = template.name.value,
                channel = template.channel.name,
                subject = template.subject,
                body = template.body.value,
                description = template.description,
                createdAt = template.createdAt.toString(),
                updatedAt = template.updatedAt.toString(),
            )
    }
}
