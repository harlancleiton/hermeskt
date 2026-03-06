package br.com.olympus.hermes.template.application.commands

import br.com.olympus.hermes.shared.application.cqrs.Command

/**
 * Command to delete an existing notification template.
 *
 * @param name The unique name of the template to delete.
 * @param channel The notification channel the template belongs to.
 */
data class DeleteTemplateCommand(
        val name: String,
        val channel: String,
) : Command
