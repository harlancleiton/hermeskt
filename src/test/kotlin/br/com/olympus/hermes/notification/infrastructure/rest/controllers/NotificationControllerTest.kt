package br.com.olympus.hermes.notification.infrastructure.rest.controllers

import arrow.core.left
import arrow.core.right
import br.com.olympus.hermes.notification.application.commands.CreateNotificationHandler
import br.com.olympus.hermes.notification.application.queries.GetNotificationQuery
import br.com.olympus.hermes.notification.application.queries.GetNotificationQueryHandler
import br.com.olympus.hermes.notification.application.queries.ListNotificationsQuery
import br.com.olympus.hermes.notification.application.queries.ListNotificationsQueryHandler
import br.com.olympus.hermes.notification.infrastructure.readmodel.NotificationView
import br.com.olympus.hermes.shared.application.repositories.PaginatedResult
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.InvalidNotificationTypeError
import br.com.olympus.hermes.shared.domain.exceptions.NotificationNotFoundError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.infrastructure.rest.exceptions.DomainException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
    fun `listNotifications should throw DomainException when invalid type is provided`() {
        val exception =
            assertThrows<DomainException> {
                controller.listNotifications(
                    page = 0,
                    size = 10,
                    status = null,
                    type = "INVALID_TYPE",
                )
            }
        assertTrue(exception.error is InvalidNotificationTypeError)
    }

    @Test
    fun `listNotifications should throw DomainException on internal error`() {
        every { listNotificationsQueryHandler.handle(any()) } returns
            PersistenceError("DB Error").left()

        val exception =
            assertThrows<DomainException> {
                controller.listNotifications(page = 1, size = 10, status = null, type = null)
            }
        assertTrue(exception.error is PersistenceError)
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
    fun `getNotification should throw DomainException when notification not found`() {
        every { getNotificationQueryHandler.handle(any()) } returns
            (null as NotificationView?).right()

        val exception = assertThrows<DomainException> { controller.getNotification("test-id") }
        assertTrue(exception.error is NotificationNotFoundError)
    }

    @Test
    fun `getNotification should throw DomainException on query left error`() {
        every { getNotificationQueryHandler.handle(any()) } returns
            NotificationNotFoundError("test-id").left()

        val exception = assertThrows<DomainException> { controller.getNotification("test-id") }
        assertTrue(exception.error is NotificationNotFoundError)
    }

    @Test
    fun `getNotification should throw DomainException on generic server error`() {
        every { getNotificationQueryHandler.handle(any()) } returns
            PersistenceError("DB Error").left()

        val exception = assertThrows<DomainException> { controller.getNotification("test-id") }
        assertTrue(exception.error is PersistenceError)
    }
}
