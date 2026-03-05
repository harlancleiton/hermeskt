package br.com.olympus.hermes.infrastructure.rest.controllers

import arrow.core.flatMap
import arrow.core.raise.either
import br.com.olympus.hermes.core.application.commands.CreateTemplateHandler
import br.com.olympus.hermes.core.application.commands.DeleteTemplateCommand
import br.com.olympus.hermes.core.application.commands.DeleteTemplateHandler
import br.com.olympus.hermes.core.application.commands.UpdateTemplateHandler
import br.com.olympus.hermes.core.application.queries.GetTemplateQuery
import br.com.olympus.hermes.core.application.queries.GetTemplateQueryHandler
import br.com.olympus.hermes.core.application.queries.ListTemplatesQuery
import br.com.olympus.hermes.core.application.queries.ListTemplatesQueryHandler
import br.com.olympus.hermes.infrastructure.rest.request.CreateTemplateRequest
import br.com.olympus.hermes.infrastructure.rest.request.UpdateTemplateRequest
import br.com.olympus.hermes.infrastructure.rest.response.TemplateResponse
import br.com.olympus.hermes.shared.domain.exceptions.ClientError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateDuplicateError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateNotFoundError
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@Path("/templates")
@Tag(name = "Templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class TemplateController(
    private val createTemplateHandler: CreateTemplateHandler,
    private val updateTemplateHandler: UpdateTemplateHandler,
    private val deleteTemplateHandler: DeleteTemplateHandler,
    private val getTemplateQueryHandler: GetTemplateQueryHandler,
    private val listTemplatesQueryHandler: ListTemplatesQueryHandler,
) {
    @POST
    @Operation(summary = "Create a template")
    @APIResponse(
        responseCode = "201",
        description = "Created",
        content = [Content(schema = Schema(implementation = TemplateResponse::class))],
    )
    fun create(request: CreateTemplateRequest): Response {
        val command = request.toCommand()
        return createTemplateHandler
            .handle(command)
            .flatMap {
                either {
                    getTemplateQueryHandler
                        .handle(
                            GetTemplateQuery(
                                name = command.name,
                                channel = command.channel,
                            ),
                        ).bind()
                }
            }.fold(
                ifLeft = { error ->
                    val status =
                        when (error) {
                            is TemplateDuplicateError -> Response.Status.CONFLICT
                            is ClientError -> Response.Status.BAD_REQUEST
                            else -> Response.Status.INTERNAL_SERVER_ERROR
                        }
                    Response
                        .status(status)
                        .entity(mapOf("message" to error.message))
                        .build()
                },
                ifRight = { template ->
                    if (template == null) {
                        Response
                            .status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(
                                mapOf(
                                    "message" to
                                        "Template not found after creation",
                                ),
                            ).build()
                    } else {
                        Response
                            .status(Response.Status.CREATED)
                            .entity(TemplateResponse.from(template))
                            .build()
                    }
                },
            )
    }

    @GET
    @Path("/{name}")
    @Operation(summary = "Get a template")
    @APIResponse(
        responseCode = "200",
        description = "OK",
        content = [Content(schema = Schema(implementation = TemplateResponse::class))],
    )
    fun get(
        @PathParam("name") name: String,
        @QueryParam("channel") channel: String,
    ): Response =
        getTemplateQueryHandler
            .handle(GetTemplateQuery(name = name, channel = channel))
            .fold(
                ifLeft = { error ->
                    val status =
                        if (error.isClientError()) {
                            Response.Status.BAD_REQUEST
                        } else {
                            Response.Status.INTERNAL_SERVER_ERROR
                        }
                    Response
                        .status(status)
                        .entity(mapOf("message" to error.message))
                        .build()
                },
                ifRight = { template ->
                    if (template == null) {
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity(mapOf("message" to "Not found"))
                            .build()
                    } else {
                        Response.ok(TemplateResponse.from(template)).build()
                    }
                },
            )

    @GET
    @Operation(summary = "List templates")
    @APIResponse(
        responseCode = "200",
        description = "OK",
        content = [Content(schema = Schema(implementation = TemplateResponse::class))],
    )
    fun list(
        @QueryParam("channel") channel: String?,
        @QueryParam("page") page: Int?,
        @QueryParam("size") size: Int?,
    ): Response {
        val resolvedPage = page ?: 0
        val resolvedSize = size ?: 20

        return listTemplatesQueryHandler
            .handle(
                ListTemplatesQuery(
                    channel = channel,
                    page = resolvedPage,
                    size = resolvedSize,
                ),
            ).fold(
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
                ifRight = { templates ->
                    Response.ok(templates.map { TemplateResponse.from(it) }).build()
                },
            )
    }

    @PUT
    @Path("/{name}")
    @Operation(summary = "Update a template")
    @APIResponse(
        responseCode = "200",
        description = "OK",
        content = [Content(schema = Schema(implementation = TemplateResponse::class))],
    )
    fun update(
        @PathParam("name") name: String,
        request: UpdateTemplateRequest,
    ): Response {
        val command = request.toCommand(name)
        return updateTemplateHandler
            .handle(command)
            .flatMap {
                either {
                    getTemplateQueryHandler
                        .handle(
                            GetTemplateQuery(
                                name = command.name,
                                channel = command.channel,
                            ),
                        ).bind()
                }
            }.fold(
                ifLeft = { error ->
                    val status =
                        when (error) {
                            is TemplateNotFoundError -> Response.Status.NOT_FOUND
                            is ClientError -> Response.Status.BAD_REQUEST
                            else -> Response.Status.INTERNAL_SERVER_ERROR
                        }
                    Response
                        .status(status)
                        .entity(mapOf("message" to error.message))
                        .build()
                },
                ifRight = { template ->
                    if (template == null) {
                        Response
                            .status(Response.Status.NOT_FOUND)
                            .entity(
                                mapOf(
                                    "message" to
                                        "Template not found after update",
                                ),
                            ).build()
                    } else {
                        Response.ok(TemplateResponse.from(template)).build()
                    }
                },
            )
    }

    @DELETE
    @Path("/{name}")
    @Operation(summary = "Delete a template")
    @APIResponse(responseCode = "204", description = "No Content")
    fun delete(
        @PathParam("name") name: String,
        @QueryParam("channel") channel: String,
    ): Response =
        deleteTemplateHandler
            .handle(DeleteTemplateCommand(name = name, channel = channel))
            .fold(
                ifLeft = { error ->
                    val status =
                        when (error) {
                            is TemplateNotFoundError -> Response.Status.NOT_FOUND
                            is ClientError -> Response.Status.BAD_REQUEST
                            else -> Response.Status.INTERNAL_SERVER_ERROR
                        }
                    Response
                        .status(status)
                        .entity(mapOf("message" to error.message))
                        .build()
                },
                ifRight = { Response.noContent().build() },
            )
}
