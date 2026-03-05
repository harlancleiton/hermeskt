package br.com.olympus.hermes.core.application.queries

import br.com.olympus.hermes.shared.application.cqrs.Query
import br.com.olympus.hermes.shared.domain.entities.NotificationTemplate

data class ListTemplatesQuery(
    val channel: String?,
    val page: Int,
    val size: Int,
) : Query<List<NotificationTemplate>>
