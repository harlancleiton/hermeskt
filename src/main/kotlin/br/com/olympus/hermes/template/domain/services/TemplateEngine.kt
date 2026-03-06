package br.com.olympus.hermes.template.domain.services

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.MissingTemplateVariablesError
import br.com.olympus.hermes.shared.domain.exceptions.TemplateNotFoundError
import br.com.olympus.hermes.template.domain.entities.ResolvedTemplate
import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName

/**
 * Domain service responsible for resolving a [TemplateName] and channel into a [ResolvedTemplate]
 * by fetching the template from the repository and interpolating placeholders with the given
 * payload.
 *
 * @param templateRepository The repository used to look up templates.
 * @param placeholderRegex The regex used to find and extract placeholder tokens in template text.
 */
class TemplateEngine(
    private val templateRepository: TemplateRepository,
    private val placeholderRegex: Regex,
) {
    /**
     * Resolves the named template for the given channel, replacing all placeholders with values
     * from [payload].
     *
     * @param templateName The name of the template to resolve.
     * @param channel The notification channel (e.g., EMAIL).
     * @param payload A map of variable names to their replacement values.
     * @return [Either.Right] with a [ResolvedTemplate], or [Either.Left] with a [BaseError].
     */
    fun resolve(
        templateName: TemplateName,
        channel: NotificationType,
        payload: Map<String, Any>,
    ): Either<BaseError, ResolvedTemplate> =
        either {
            val template =
                templateRepository.findByNameAndChannel(templateName, channel).bind()
                    ?: raise(TemplateNotFoundError(templateName.value, channel.name))

            val bodyPlaceholders = extractPlaceholders(template.body.value)
            val subjectPlaceholders = template.subject?.let { extractPlaceholders(it) }.orEmpty()
            val placeholders = (bodyPlaceholders + subjectPlaceholders).distinct()

            val missing = placeholders.filterNot { payload.containsKey(it) }
            ensure(missing.isEmpty()) { MissingTemplateVariablesError(missing) }

            val resolvedBody = interpolate(template.body.value, payload)
            val resolvedSubject = template.subject?.let { interpolate(it, payload) }

            ResolvedTemplate(body = resolvedBody, subject = resolvedSubject)
        }

    private fun extractPlaceholders(source: String): List<String> =
        placeholderRegex.findAll(source).map { match -> match.groupValues[1] }.toList()

    private fun interpolate(
        source: String,
        payload: Map<String, Any>,
    ): String =
        placeholderRegex.replace(source) { match ->
            val key = match.groupValues[1]
            payload[key].toString()
        }
}
