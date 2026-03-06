package br.com.olympus.hermes.shared.infrastructure.rest.response

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Standardised JSON error response body for all REST endpoints.
 *
 * @property message Human-readable description of the error.
 * @property errors Optional list of individual validation error messages (populated only for
 * [ValidationErrors]).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val message: String,
    val errors: List<String>? = null,
)
