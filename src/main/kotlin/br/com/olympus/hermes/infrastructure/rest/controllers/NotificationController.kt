package br.com.olympus.hermes.infrastructure.rest.controllers

import arrow.core.flatMap
import br.com.olympus.hermes.core.application.commands.CreateNotificationHandler
import br.com.olympus.hermes.core.application.queries.GetNotificationQuery
import br.com.olympus.hermes.core.application.queries.GetNotificationQueryHandler
import br.com.olympus.hermes.core.application.queries.ListNotificationsQuery
import br.com.olympus.hermes.core.application.queries.ListNotificationsQueryHandler
import br.com.olympus.hermes.infrastructure.rest.request.CreateEmailNotificationRequest
import br.com.olympus.hermes.infrastructure.rest.request.CreatePushNotificationRequest
import br.com.olympus.hermes.infrastructure.rest.request.CreateSmsNotificationRequest
import br.com.olympus.hermes.infrastructure.rest.request.CreateWhatsAppNotificationRequest
import br.com.olympus.hermes.infrastructure.rest.response.NotificationViewResponse
import br.com.olympus.hermes.infrastructure.rest.response.PaginatedNotificationResponse
import br.com.olympus.hermes.shared.domain.exceptions.ClientError
import br.com.olympus.hermes.shared.domain.exceptions.NotificationNotFoundError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses

/**
 * REST resource exposing notification management endpoints.
 *
 * @property createNotificationHandler Handler for the notification creation command.
 */
@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class NotificationController(
    private val createNotificationHandler: CreateNotificationHandler,
    private val getNotificationQueryHandler: GetNotificationQueryHandler,
    private val listNotificationsQueryHandler: ListNotificationsQueryHandler,
) {
    /**
     * Creates a new email notification.
     *
     * @param request The request body containing the email notification details.
     * @return 201 Created with the notification id, or 400/500 on failure.
     */
    @POST
    @Path("/email")
    @Operation(
        summary = "Create an email notification",
        description = "Sends a new email notification to the given recipient.",
    )
    @APIResponses(
        APIResponse(
            responseCode = "201",
            description = "Notification created successfully",
        ),
        APIResponse(responseCode = "400", description = "Invalid request payload"),
        APIResponse(responseCode = "500", description = "Server-side failure"),
    )
    @RequestBody(
        required = true,
        content =
            [
                Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema =
                        Schema(
                            implementation =
                                CreateEmailNotificationRequest::class,
                        ),
                ),
            ],
    )
    fun createEmailNotification(request: CreateEmailNotificationRequest): Response =
        request
            .toCommand()
            .flatMap { command ->
                createNotificationHandler.handle(command).map {
                    mapOf(
                        "id" to command.id,
                    )
                }
            }.fold(
                ifLeft = { error ->
                    val status =
                        if (error is ClientError) {
                            Response.Status.BAD_REQUEST
                        } else {
                            Response.Status.INTERNAL_SERVER_ERROR
                        }
                    Response
                        .status(status)
                        .entity(mapOf("message" to error.message))
                        .build()
                },
                ifRight = { response ->
                    Response
                        .status(Response.Status.CREATED)
                        .entity(response)
                        .build()
                },
            )

    /**
     * Creates a new push notification.
     *
     * @param request The request body containing the push notification details.
     * @return 201 Created with the notification id, or 400/500 on failure.
     */
    @POST
    @Path("/push")
    @Operation(
        summary = "Create a push notification",
        description = "Sends a new push notification to the given device token.",
    )
    @APIResponses(
        APIResponse(
            responseCode = "201",
            description = "Notification created successfully",
        ),
        APIResponse(responseCode = "400", description = "Invalid request payload"),
        APIResponse(responseCode = "500", description = "Server-side failure"),
    )
    @RequestBody(
        required = true,
        content =
            [
                Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema =
                        Schema(
                            implementation =
                                CreatePushNotificationRequest::class,
                        ),
                ),
            ],
    )
    fun createPushNotification(request: CreatePushNotificationRequest): Response =
        request
            .toCommand()
            .flatMap { command ->
                createNotificationHandler.handle(command).map {
                    mapOf(
                        "id" to command.id,
                    )
                }
            }.fold(
                ifLeft = { error ->
                    val status =
                        if (error is ClientError) {
                            Response.Status.BAD_REQUEST
                        } else {
                            Response.Status.INTERNAL_SERVER_ERROR
                        }
                    Response
                        .status(status)
                        .entity(mapOf("message" to error.message))
                        .build()
                },
                ifRight = { response ->
                    Response
                        .status(Response.Status.CREATED)
                        .entity(response)
                        .build()
                },
            )

    /**
     * Creates a new SMS notification.
     *
     * @param request The request body containing the SMS notification details.
     * @return 201 Created with the notification id, or 400/500 on failure.
     */
    @POST
    @Path("/sms")
    @Operation(
        summary = "Create an SMS notification",
        description = "Sends a new SMS notification to the given phone number.",
    )
    @APIResponses(
        APIResponse(
            responseCode = "201",
            description = "Notification created successfully",
        ),
        APIResponse(responseCode = "400", description = "Invalid request payload"),
        APIResponse(responseCode = "500", description = "Server-side failure"),
    )
    @RequestBody(
        required = true,
        content =
            [
                Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema =
                        Schema(
                            implementation =
                                CreateSmsNotificationRequest::class,
                        ),
                ),
            ],
    )
    fun createSmsNotification(request: CreateSmsNotificationRequest): Response =
        request
            .toCommand()
            .flatMap { command ->
                createNotificationHandler.handle(command).map {
                    mapOf(
                        "id" to command.id,
                    )
                }
            }.fold(
                ifLeft = { error ->
                    val status =
                        if (error is ClientError) {
                            Response.Status.BAD_REQUEST
                        } else {
                            Response.Status.INTERNAL_SERVER_ERROR
                        }
                    Response
                        .status(status)
                        .entity(mapOf("message" to error.message))
                        .build()
                },
                ifRight = { response ->
                    Response
                        .status(Response.Status.CREATED)
                        .entity(response)
                        .build()
                },
            )

    /**
     * Creates a new WhatsApp notification.
     *
     * @param request The request body containing the WhatsApp notification details.
     * @return 201 Created with the notification id, or 400/500 on failure.
     */
    @POST
    @Path("/whatsapp")
    @Operation(
        summary = "Create a WhatsApp notification",
        description =
            "Sends a new WhatsApp notification to the given phone number using a Business API template.",
    )
    @APIResponses(
        APIResponse(
            responseCode = "201",
            description = "Notification created successfully",
        ),
        APIResponse(responseCode = "400", description = "Invalid request payload"),
        APIResponse(responseCode = "500", description = "Server-side failure"),
    )
    @RequestBody(
        required = true,
        content =
            [
                Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema =
                        Schema(
                            implementation =
                                CreateWhatsAppNotificationRequest::class,
                        ),
                ),
            ],
    )
    fun createWhatsAppNotification(request: CreateWhatsAppNotificationRequest): Response =
        request
            .toCommand()
            .flatMap { command ->
                createNotificationHandler.handle(command).map {
                    mapOf(
                        "id" to command.id,
                    )
                }
            }.fold(
                ifLeft = { error ->
                    val status =
                        if (error is ClientError) {
                            Response.Status.BAD_REQUEST
                        } else {
                            Response.Status.INTERNAL_SERVER_ERROR
                        }
                    Response
                        .status(status)
                        .entity(mapOf("message" to error.message))
                        .build()
                },
                ifRight = { response ->
                    Response
                        .status(Response.Status.CREATED)
                        .entity(response)
                        .build()
                },
            )

    /**
     * Retrieves a paginated list of notifications.
     *
     * @param page The page index to retrieve (0-based).
     * @param size The number of items per page.
     * @param status Optional filter by notification status.
     * @param type Optional filter by notification type.
     * @return 200 OK with the paginated list of notifications.
     */
    @GET
    @Operation(
        summary = "List notifications",
        description =
            "Retrieves a paginated list of notifications, optionally filtered by status and type.",
    )
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "Notifications retrieved successfully",
        ),
        APIResponse(responseCode = "500", description = "Server-side failure"),
    )
    fun listNotifications(
        @QueryParam("page") @DefaultValue("0") page: Int,
        @QueryParam("size") @DefaultValue("10") size: Int,
        @QueryParam("status") status: String?,
        @QueryParam("type") type: String?,
    ): Response {
        val notificationType =
            type?.let {
                try {
                    NotificationType.valueOf(it.uppercase())
                } catch (e: IllegalArgumentException) {
                    return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(
                            mapOf(
                                "message" to
                                    "Invalid notification type: $it",
                            ),
                        ).build()
                }
            }

        val query =
            ListNotificationsQuery(
                status = status,
                type = notificationType?.name,
                page = page,
                size = size,
            )

        return listNotificationsQueryHandler
            .handle(query)
            .fold(
                ifLeft = { error ->
                    Response
                        .status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(mapOf("message" to error.message))
                        .build()
                },
                ifRight = { result ->
                    Response
                        .ok(PaginatedNotificationResponse.from(result))
                        .build()
                },
            )
    }

    /**
     * Retrieves a single notification by its aggregate ID.
     *
     * @param id The ID of the notification.
     * @return 200 OK with the notification, or 404 Not Found if it doesn't exist.
     */
    @GET
    @Path("/{id}")
    @Operation(
        summary = "Get notification by ID",
        description =
            "Retrieves a specific notification by its unique aggregate identifier.",
    )
    @APIResponses(
        APIResponse(responseCode = "200", description = "Notification found"),
        APIResponse(responseCode = "404", description = "Notification not found"),
        APIResponse(responseCode = "500", description = "Server-side failure"),
    )
    fun getNotification(
        @PathParam("id") id: String,
    ): Response {
        val query = GetNotificationQuery(id)

        return getNotificationQueryHandler
            .handle(query)
            .fold(
                ifLeft = { error ->
                    val status =
                        when (error) {
                            is NotificationNotFoundError ->
                                Response.Status.NOT_FOUND
                            else ->
                                if (error.isClientError()) {
                                    Response.Status.BAD_REQUEST
                                } else {
                                    Response.Status
                                        .INTERNAL_SERVER_ERROR
                                }
                        }
                    Response
                        .status(status)
                        .entity(mapOf("message" to error.message))
                        .build()
                },
                ifRight = { notificationView ->
                    if (notificationView != null) {
                        Response
                            .ok(
                                NotificationViewResponse.from(
                                    notificationView,
                                ),
                            ).build()
                    } else {
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity(
                                mapOf(
                                    "message" to
                                        "Notification not found",
                                ),
                            ).build()
                    }
                },
            )
    }
}
