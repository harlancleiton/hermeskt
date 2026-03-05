package br.com.olympus.hermes.infrastructure.rest.controllers

import arrow.core.flatMap
import br.com.olympus.hermes.core.application.commands.CreateNotificationHandler
import br.com.olympus.hermes.infrastructure.rest.request.CreateEmailNotificationRequest
import br.com.olympus.hermes.infrastructure.rest.request.CreatePushNotificationRequest
import br.com.olympus.hermes.shared.domain.exceptions.ClientError
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

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
     * @return 201 Created with the [NotificationResponse] body, or 400/500 on failure.
     */
    @POST
    @Path("/email")
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
     * @return 201 Created with the [NotificationResponse] body, or 400/500 on failure.
     */
    @POST
    @Path("/push")
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
}
