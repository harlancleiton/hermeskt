package br.com.olympus.hermes.core.application.commands

import br.com.olympus.hermes.shared.application.cqrs.Command

data class UpdateTemplateCommand(
    val name: String,
    val channel: String,
    val subject: String?,
    val body: String?,
    val description: String?,
) : Command
