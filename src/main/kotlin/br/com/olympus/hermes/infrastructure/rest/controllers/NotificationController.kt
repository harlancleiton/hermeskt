package br.com.olympus.hermes.infrastructure.rest.controllers

import arrow.core.flatMap
import br.com.olympus.hermes.core.application.commands.CreateNotificationHandler
import br.com.olympus.hermes.infrastructure.rest.request.CreateEmailNotificationRequest
import br.com.olympus.hermes.infrastructure.rest.request.CreatePushNotificationRequest
import br.com.olympus.hermes.infrastructure.rest.request.CreateSmsNotificationRequest
import br.com.olympus.hermes.infrastructure.rest.request.CreateWhatsAppNotificationRequest
import br.com.olympus.hermes.shared.domain.exceptions.ClientError
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
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
        APIResponse(responseCode = "201", description = "Notification created successfully"),
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
                    Response.status(Response.Status.CREATED).entity(response).build()
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
        APIResponse(responseCode = "201", description = "Notification created successfully"),
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
                    Response.status(Response.Status.CREATED).entity(response).build()
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
        APIResponse(responseCode = "201", description = "Notification created successfully"),
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
                    Response.status(Response.Status.CREATED).entity(response).build()
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
        APIResponse(responseCode = "201", description = "Notification created successfully"),
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
                    Response.status(Response.Status.CREATED).entity(response).build()
                },
            )
}
