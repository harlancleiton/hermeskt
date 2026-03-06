package br.com.olympus.hermes.template.domain.entities

/**
 * Represents the result of resolving a [NotificationTemplate] with concrete variable values.
 *
 * @param body The fully interpolated body content with all placeholders replaced.
 * @param subject The fully interpolated subject line, or null if not applicable.
 */
data class ResolvedTemplate(
        val body: String,
        val subject: String?,
)
