package br.com.olympus.hermes.template.application.commands

import br.com.olympus.hermes.shared.application.cqrs.Command

/**
 * Command to update an existing notification template.
 *
 * @param name The unique name identifying the template to update.
 * @param channel The notification channel the template belongs to.
 * @param subject The new subject line (null to leave unchanged).
 * @param body The new body content (null to leave unchanged).
 * @param description The new description (null to leave unchanged).
 */
data class UpdateTemplateCommand(
    val name: String,
    val channel: String,
    val subject: String?,
    val body: String?,
    val description: String?,
) : Command
