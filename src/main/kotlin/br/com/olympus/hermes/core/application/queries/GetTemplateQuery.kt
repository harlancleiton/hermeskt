package br.com.olympus.hermes.core.application.queries

import br.com.olympus.hermes.shared.application.cqrs.Query
import br.com.olympus.hermes.shared.domain.entities.NotificationTemplate

data class GetTemplateQuery(
    val name: String,
    val channel: String,
) : Query<NotificationTemplate?>
