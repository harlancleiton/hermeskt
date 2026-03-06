package br.com.olympus.hermes.template.domain.entities

import br.com.olympus.hermes.notification.domain.factories.NotificationType
import br.com.olympus.hermes.template.domain.valueobjects.TemplateBody
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import java.time.Instant

/**
 * Represents a notification template that can be used to generate notifications.
 *
 * @param name The unique name identifying this template.
 * @param channel The notification channel (e.g., EMAIL, SMS).
 * @param subject The subject line, applicable to email-based notifications.
 * @param body The body content of the template, potentially with placeholders.
 * @param description An optional human-readable description of the template's purpose.
 * @param createdAt The timestamp when this template was created.
 * @param updatedAt The timestamp when this template was last updated.
 */
data class NotificationTemplate(
    val name: TemplateName,
    val channel: NotificationType,
    val subject: String?,
    val body: TemplateBody,
    val description: String?,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
