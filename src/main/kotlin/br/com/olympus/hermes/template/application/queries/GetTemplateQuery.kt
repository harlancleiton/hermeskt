package br.com.olympus.hermes.template.application.queries

import br.com.olympus.hermes.shared.application.cqrs.Query
import br.com.olympus.hermes.template.domain.entities.NotificationTemplate

/**
 * Query to retrieve a single notification template by name and channel.
 *
 * @param name The unique name of the template.
 * @param channel The notification channel the template belongs to.
 */
data class GetTemplateQuery(
    val name: String,
    val channel: String,
) : Query<NotificationTemplate?>
