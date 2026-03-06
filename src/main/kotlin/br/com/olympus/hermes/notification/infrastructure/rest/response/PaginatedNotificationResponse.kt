package br.com.olympus.hermes.notification.infrastructure.rest.response

import br.com.olympus.hermes.notification.infrastructure.readmodel.NotificationView
import br.com.olympus.hermes.shared.application.repositories.PaginatedResult

/** HTTP response DTO for paginated notification queries. */
data class PaginatedNotificationResponse(
    val items: List<NotificationViewResponse>,
    val totalCount: Long,
    val pageIndex: Int,
    val pageSize: Int,
    val totalPages: Int,
) {
    companion object {
        fun from(result: PaginatedResult<NotificationView>): PaginatedNotificationResponse =
            PaginatedNotificationResponse(
                items = result.items.map { NotificationViewResponse.from(it) },
                totalCount = result.totalCount,
                pageIndex = result.pageIndex,
                pageSize = result.pageSize,
                totalPages = result.totalPages,
            )
    }
}
