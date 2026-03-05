package br.com.olympus.hermes.shared.domain.repositories

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.entities.NotificationTemplate
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.factories.NotificationType
import br.com.olympus.hermes.shared.domain.valueobjects.TemplateName

interface TemplateRepository {
    fun findByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, NotificationTemplate?>

    fun findAllByChannel(
        channel: NotificationType?,
        page: Int,
        size: Int,
    ): Either<BaseError, List<NotificationTemplate>>

    fun save(template: NotificationTemplate): Either<BaseError, NotificationTemplate>

    fun update(template: NotificationTemplate): Either<BaseError, NotificationTemplate>

    fun deleteByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, Boolean>

    fun existsByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, Boolean>
}
