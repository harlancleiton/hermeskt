package br.com.olympus.hermes.infrastructure.rest.exceptions

import br.com.olympus.hermes.infrastructure.rest.response.ErrorResponse
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.ClientError
import br.com.olympus.hermes.shared.domain.exceptions.NotificationNotFoundError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateDuplicateError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateNotFoundError
import br.com.olympus.hermes.shared.domain.exceptions.ValidationErrors
import io.quarkus.logging.Log
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

/**
 * Global JAX-RS exception mapper that translates [DomainException] instances into standardised HTTP
 * error responses.
 *
 * Status code resolution rules:
 * - [NotificationNotFoundError], [TemplateNotFoundError] → 404 Not Found
 * - [TemplateDuplicateError] → 409 Conflict
 * - [ValidationErrors] → 422 Unprocessable Entity
 * - Any other [ClientError] → 400 Bad Request
 * - Any [ServerError] → 500 Internal Server Error
 */
@Provider
class DomainExceptionMapper : ExceptionMapper<DomainException> {
    override fun toResponse(exception: DomainException): Response {
        val error = exception.error
        val status = resolveStatus(error)
        val body = buildErrorResponse(error)

        if (error.isServerError()) {
            Log.error("Server error: ${error.message}", exception)
        } else {
            Log.debug("Client error [${status.statusCode}]: ${error.message}")
        }

        return Response
            .status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(body)
            .build()
    }

    private fun resolveStatus(error: BaseError): Response.Status =
        when (error) {
            is NotificationNotFoundError -> Response.Status.NOT_FOUND
            is TemplateNotFoundError -> Response.Status.NOT_FOUND
            is TemplateDuplicateError -> Response.Status.CONFLICT
            is ValidationErrors ->
                Response.Status.fromStatusCode(422)
                    ?: Response.Status.BAD_REQUEST
            is ClientError -> Response.Status.BAD_REQUEST
            else -> Response.Status.INTERNAL_SERVER_ERROR
        }

    private fun buildErrorResponse(error: BaseError): ErrorResponse =
        when (error) {
            is ValidationErrors ->
                ErrorResponse(
                    message = error.message,
                    errors = error.errors.map { it.message },
                )
            else -> ErrorResponse(message = error.message)
        }
}
