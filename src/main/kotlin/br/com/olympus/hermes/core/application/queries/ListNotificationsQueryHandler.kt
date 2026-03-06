package br.com.olympus.hermes.core.application.queries

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.application.cqrs.QueryHandler
import br.com.olympus.hermes.shared.application.repositories.PaginatedResult
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ListNotificationsQueryHandler(
    private val viewRepository: NotificationViewRepository,
) : QueryHandler<ListNotificationsQuery, PaginatedResult<NotificationView>> {
    @WithSpan("notification.query.list")
    override fun handle(query: ListNotificationsQuery): Either<BaseError, PaginatedResult<NotificationView>> =
        either {
            Span.current().apply {
                setAttribute("notification.status.filter", query.status ?: "<all>")
                setAttribute("notification.type.filter", query.type ?: "<all>")
                setAttribute("query.page", query.page.toLong())
                setAttribute("query.size", query.size.toLong())
            }
            Log.debug(
                "Listing notifications status=${query.status ?: "<all>"} type=${query.type ?: "<all>"} page=${query.page} size=${query.size}",
            )

            val result =
                viewRepository
                    .findAll(
                        pageIndex = query.page,
                        pageSize = query.size,
                        status = query.status?.trim()?.uppercase(),
                        type = query.type?.trim()?.uppercase(),
                    ).bind()
            Span.current().setAttribute("notification.result.count", result.totalCount)
            result
        }
}
