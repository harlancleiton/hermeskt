package br.com.olympus.hermes.infrastructure.rest.response

import br.com.olympus.hermes.shared.domain.repositories.PaginatedResult
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView

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
