package br.com.olympus.hermes.core.application.queries

import br.com.olympus.hermes.shared.application.cqrs.Query
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView

/**
 * Query to retrieve a single notification view by its aggregate identifier.
 *
 * @property id The aggregate identifier of the notification to retrieve.
 */
data class GetNotificationQuery(
    val id: String,
) : Query<NotificationView?>
