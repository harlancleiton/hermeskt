package br.com.olympus.hermes.shared.domain.repositories

/**
 * Represents a paginated result set of items.
 *
 * @param T The type of items in the result set.
 * @property items The list of items for the current page.
 * @property totalCount The total number of items across all pages.
 * @property pageIndex The current page index (0-based).
 * @property pageSize The number of items per page.
 */
data class PaginatedResult<T>(
    val items: List<T>,
    val totalCount: Long,
    val pageIndex: Int,
    val pageSize: Int,
) {
    /** The total number of pages. */
    val totalPages: Int
        get() =
            if (pageSize > 0) {
                ((totalCount + pageSize - 1) / pageSize).toInt()
            } else {
                0
            }
}
