package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.valueobjects.TemplateBody
import br.com.olympus.hermes.shared.domain.valueobjects.TemplateName
import java.util.Date

data class NotificationTemplate(
    val name: TemplateName,
    val channel: NotificationType,
    val subject: String?,
    val body: TemplateBody,
    val description: String?,
    val createdAt: Date,
    val updatedAt: Date,
)
