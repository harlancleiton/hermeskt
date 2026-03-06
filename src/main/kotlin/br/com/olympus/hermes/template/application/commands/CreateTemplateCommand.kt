package br.com.olympus.hermes.template.application.commands

import br.com.olympus.hermes.shared.application.cqrs.Command

/**
 * Command to create a new notification template.
 *
 * @param name The unique name for the template.
 * @param channel The notification channel (e.g., EMAIL, SMS).
 * @param subject The subject line, applicable to email-based notifications.
 * @param body The body content of the template, potentially with placeholders.
 * @param description An optional human-readable description of the template's purpose.
 */
data class CreateTemplateCommand(
    val name: String,
    val channel: String,
    val subject: String?,
    val body: String,
    val description: String?,
) : Command
