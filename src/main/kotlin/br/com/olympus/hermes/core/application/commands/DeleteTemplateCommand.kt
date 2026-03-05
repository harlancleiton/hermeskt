package br.com.olympus.hermes.core.application.commands

import br.com.olympus.hermes.shared.application.cqrs.Command

data class DeleteTemplateCommand(
    val name: String,
    val channel: String,
) : Command
