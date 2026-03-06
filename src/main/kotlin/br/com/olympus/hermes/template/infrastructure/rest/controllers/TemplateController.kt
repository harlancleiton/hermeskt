package br.com.olympus.hermes.template.infrastructure.rest.controllers

import br.com.olympus.hermes.infrastructure.rest.exceptions.DomainException
import br.com.olympus.hermes.infrastructure.rest.extensions.getOrThrowDomain
import br.com.olympus.hermes.shared.domain.exceptions.TemplateNotFoundError
import br.com.olympus.hermes.template.application.commands.CreateTemplateHandler
import br.com.olympus.hermes.template.application.commands.DeleteTemplateCommand
import br.com.olympus.hermes.template.application.commands.DeleteTemplateHandler
import br.com.olympus.hermes.template.application.commands.UpdateTemplateHandler
import br.com.olympus.hermes.template.application.queries.GetTemplateQuery
import br.com.olympus.hermes.template.application.queries.GetTemplateQueryHandler
import br.com.olympus.hermes.template.application.queries.ListTemplatesQuery
import br.com.olympus.hermes.template.application.queries.ListTemplatesQueryHandler
import br.com.olympus.hermes.template.infrastructure.rest.request.CreateTemplateRequest
import br.com.olympus.hermes.template.infrastructure.rest.request.UpdateTemplateRequest
import br.com.olympus.hermes.template.infrastructure.rest.response.TemplateResponse
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

/**
 * REST resource exposing template CRUD endpoints.
 *
 * Error handling is delegated to [DomainExceptionMapper] via [DomainException].
 */
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
        createTemplateHandler.handle(command).getOrThrowDomain()

        val template =
                getTemplateQueryHandler
                        .handle(GetTemplateQuery(name = command.name, channel = command.channel))
                        .getOrThrowDomain()
                        ?: throw DomainException(
                                TemplateNotFoundError(
                                        name = command.name,
                                        channel = command.channel,
                                ),
                        )

        return Response.status(Response.Status.CREATED)
                .entity(TemplateResponse.from(template))
                .build()
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
    ): Response {
        val template =
                getTemplateQueryHandler
                        .handle(GetTemplateQuery(name = name, channel = channel))
                        .getOrThrowDomain()
                        ?: throw DomainException(
                                TemplateNotFoundError(name = name, channel = channel),
                        )

        return Response.ok(TemplateResponse.from(template)).build()
    }

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

        val templates =
                listTemplatesQueryHandler
                        .handle(
                                ListTemplatesQuery(
                                        channel = channel,
                                        page = resolvedPage,
                                        size = resolvedSize,
                                ),
                        )
                        .getOrThrowDomain()

        return Response.ok(templates.map { TemplateResponse.from(it) }).build()
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
        updateTemplateHandler.handle(command).getOrThrowDomain()

        val template =
                getTemplateQueryHandler
                        .handle(GetTemplateQuery(name = command.name, channel = command.channel))
                        .getOrThrowDomain()
                        ?: throw DomainException(
                                TemplateNotFoundError(
                                        name = command.name,
                                        channel = command.channel,
                                ),
                        )

        return Response.ok(TemplateResponse.from(template)).build()
    }

    @DELETE
    @Path("/{name}")
    @Operation(summary = "Delete a template")
    @APIResponse(responseCode = "204", description = "No Content")
    fun delete(
            @PathParam("name") name: String,
            @QueryParam("channel") channel: String,
    ): Response {
        deleteTemplateHandler
                .handle(DeleteTemplateCommand(name = name, channel = channel))
                .getOrThrowDomain()
        return Response.noContent().build()
    }
}
