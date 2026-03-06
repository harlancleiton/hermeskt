package br.com.olympus.hermes.infrastructure.rest.controllers

import arrow.core.left
import arrow.core.right
import br.com.olympus.hermes.core.application.commands.CreateNotificationHandler
import br.com.olympus.hermes.core.application.queries.GetNotificationQuery
import br.com.olympus.hermes.core.application.queries.GetNotificationQueryHandler
import br.com.olympus.hermes.core.application.queries.ListNotificationsQuery
import br.com.olympus.hermes.core.application.queries.ListNotificationsQueryHandler
import br.com.olympus.hermes.shared.domain.exceptions.NotificationNotFoundError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.repositories.PaginatedResult
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NotificationControllerTest {
    private lateinit var createNotificationHandler: CreateNotificationHandler
    private lateinit var getNotificationQueryHandler: GetNotificationQueryHandler
    private lateinit var listNotificationsQueryHandler: ListNotificationsQueryHandler
    private lateinit var controller: NotificationController

    @BeforeEach
    fun setUp() {
        createNotificationHandler = mockk()
        getNotificationQueryHandler = mockk()
        listNotificationsQueryHandler = mockk()
        controller =
            NotificationController(
                createNotificationHandler,
                getNotificationQueryHandler,
                listNotificationsQueryHandler,
            )
    }

    @Test
    fun `listNotifications should return 200 with paginated results on success`() {
        val view =
            NotificationView().apply {
                id = "test-id"
                status = "PENDING"
                type = NotificationType.SMS.name
                content = "Testing"
                from = "system"
                to = "11999999999"
                createdAt = java.util.Date()
                updatedAt = java.util.Date()
            }
        val paginatedResult =
            PaginatedResult(
                items = listOf(view),
                pageIndex = 1,
                pageSize = 10,
                totalCount = 1,
            )

        every { listNotificationsQueryHandler.handle(any()) } returns paginatedResult.right()

        val response =
            controller.listNotifications(page = 1, size = 10, status = "PENDING", type = "SMS")

        assertEquals(Response.Status.OK.statusCode, response.status)
        verify {
            listNotificationsQueryHandler.handle(
                ListNotificationsQuery(status = "PENDING", type = "SMS", page = 1, size = 10),
            )
        }
    }

    @Test
    fun `listNotifications should return 400 when invalid type is provided`() {
        val response =
            controller.listNotifications(
                page = 0,
                size = 10,
                status = null,
                type = "INVALID_TYPE",
            )
        assertEquals(Response.Status.BAD_REQUEST.statusCode, response.status)
    }

    @Test
    fun `listNotifications should return 500 on internal error`() {
        every { listNotificationsQueryHandler.handle(any()) } returns
            PersistenceError("DB Error").left()

        val response = controller.listNotifications(page = 1, size = 10, status = null, type = null)

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.statusCode, response.status)
    }

    @Test
    fun `getNotification should return 200 on success`() {
        val view =
            NotificationView().apply {
                id = "test-id"
                status = "PENDING"
                type = NotificationType.PUSH.name
                content = "Testing"
                from = "system"
                to = "device-token"
                createdAt = java.util.Date()
                updatedAt = java.util.Date()
            }

        every { getNotificationQueryHandler.handle(any()) } returns view.right()

        val response = controller.getNotification("test-id")

        assertEquals(Response.Status.OK.statusCode, response.status)
        verify { getNotificationQueryHandler.handle(GetNotificationQuery("test-id")) }
    }

    @Test
    fun `getNotification should return 404 when notification not found`() {
        every { getNotificationQueryHandler.handle(any()) } returns
            NotificationNotFoundError("test-id").left()

        val response = controller.getNotification("test-id")

        assertEquals(Response.Status.NOT_FOUND.statusCode, response.status)
    }

    @Test
    fun `getNotification should return 500 on generic server error`() {
        every { getNotificationQueryHandler.handle(any()) } returns
            PersistenceError("DB Error").left()

        val response = controller.getNotification("test-id")

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.statusCode, response.status)
    }
}
