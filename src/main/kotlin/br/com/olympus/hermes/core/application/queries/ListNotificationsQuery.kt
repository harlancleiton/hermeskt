package br.com.olympus.hermes.core.application.queries

import br.com.olympus.hermes.shared.application.cqrs.Query
import br.com.olympus.hermes.shared.application.repositories.PaginatedResult
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView

data class ListNotificationsQuery(
    val status: String?,
    val type: String?,
    val page: Int,
    val size: Int,
) : Query<PaginatedResult<NotificationView>>
