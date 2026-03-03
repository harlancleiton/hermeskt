package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.domain.entities.EmailNotification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.valueobjects.Email
import br.com.olympus.hermes.shared.domain.valueobjects.EmailSubject
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass

/** Converts [EmailNotification] domain entities to/from [NotificationRecord] DynamoDB items. */
class EmailNotificationRecordConverter : NotificationRecordConverter<EmailNotification> {
    override val type: String = TYPE
    override val entityClass: KClass<EmailNotification> = EmailNotification::class

    override fun populateRecord(
        notification: EmailNotification,
        record: NotificationRecord,
        objectMapper: ObjectMapper,
    ) {
        record.writeCommonFields(notification, type, objectMapper)
        record.fromEmail = notification.from.value
        record.toEmail = notification.to.value
        record.subject = notification.subject.subject
    }

    override fun fromRecord(
        record: NotificationRecord,
        objectMapper: ObjectMapper,
    ): Either<BaseError, EmailNotification> =
        either {
            val entityId = readEntityId(record)
            val from = Email.from(requireField(record.fromEmail, "fromEmail", TYPE)).bind()
            val to = Email.from(requireField(record.toEmail, "toEmail", TYPE)).bind()
            val subject = EmailSubject.create(requireField(record.subject, "subject", TYPE)).bind()
            val dates = record.readDates()

            EmailNotification(
                content = record.content,
                payload = record.deserializePayload(objectMapper),
                shippingReceipt = record.deserializeShippingReceipt(objectMapper),
                sentAt = dates.sentAt,
                deliveryAt = dates.deliveryAt,
                seenAt = dates.seenAt,
                id = entityId,
                createdAt = dates.createdAt,
                updatedAt = dates.updatedAt,
                from = from,
                to = to,
                subject = subject,
                isNew = false,
            ).also { it.version = record.version }
        }

    companion object {
        const val TYPE = "EMAIL"
    }
}
