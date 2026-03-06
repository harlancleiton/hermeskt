package br.com.olympus.hermes.notification.application.queries

import br.com.olympus.hermes.notification.infrastructure.readmodel.NotificationView
import br.com.olympus.hermes.shared.application.cqrs.Query
import br.com.olympus.hermes.shared.application.repositories.PaginatedResult

data class ListNotificationsQuery(
    val status: String?,
    val type: String?,
    val page: Int,
    val size: Int,
) : Query<PaginatedResult<NotificationView>>
