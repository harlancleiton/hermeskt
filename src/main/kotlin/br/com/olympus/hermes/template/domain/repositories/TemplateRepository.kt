package br.com.olympus.hermes.template.domain.repositories

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.template.domain.entities.NotificationTemplate
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName

/**
 * Port interface for persisting and retrieving [NotificationTemplate] entities.
 *
 * Implementations live in the infrastructure layer.
 */
interface TemplateRepository {
    /**
     * Finds a template by its name and channel.
     *
     * @param name The template name value object.
     * @param channel The notification channel type.
     * @return [Either.Right] with the nullable [NotificationTemplate], or [Either.Left] with an
     * error.
     */
    fun findByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, NotificationTemplate?>

    /**
     * Lists all templates optionally filtered by channel, with pagination.
     *
     * @param channel Optional channel filter.
     * @param page Zero-based page number.
     * @param size Page size.
     * @return [Either.Right] with the list of templates, or [Either.Left] with an error.
     */
    fun findAllByChannel(
        channel: NotificationType?,
        page: Int,
        size: Int,
    ): Either<BaseError, List<NotificationTemplate>>

    /**
     * Saves a new template.
     *
     * @param template The template to save.
     * @return [Either.Right] with the saved [NotificationTemplate], or [Either.Left] with an error.
     */
    fun save(template: NotificationTemplate): Either<BaseError, NotificationTemplate>

    /**
     * Updates an existing template.
     *
     * @param template The template with updated fields.
     * @return [Either.Right] with the updated [NotificationTemplate], or [Either.Left] with an
     * error.
     */
    fun update(template: NotificationTemplate): Either<BaseError, NotificationTemplate>

    /**
     * Deletes a template by its name and channel.
     *
     * @param name The template name value object.
     * @param channel The notification channel type.
     * @return [Either.Right] with true if deleted, or [Either.Left] with an error.
     */
    fun deleteByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, Boolean>

    /**
     * Checks whether a template with the given name and channel exists.
     *
     * @param name The template name value object.
     * @param channel The notification channel type.
     * @return [Either.Right] with true if exists, false otherwise, or [Either.Left] with an error.
     */
    fun existsByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, Boolean>
}
