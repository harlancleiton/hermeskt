package br.com.olympus.hermes.template.application.queries

import br.com.olympus.hermes.shared.application.cqrs.Query
import br.com.olympus.hermes.template.domain.entities.NotificationTemplate

/**
 * Query to list notification templates, optionally filtered by channel.
 *
 * @param channel Optional notification channel to filter results (null means all channels).
 * @param page Zero-based page index for pagination.
 * @param size Number of results per page.
 */
data class ListTemplatesQuery(
        val channel: String?,
        val page: Int,
        val size: Int,
) : Query<List<NotificationTemplate>>
