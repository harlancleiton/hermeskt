package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.shared.domain.entities.SmsNotification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass

/** Converts [SmsNotification] domain entities to/from [NotificationRecord] DynamoDB items. */
class SmsNotificationRecordConverter : NotificationRecordConverter<SmsNotification> {
    override val type: String = TYPE
    override val entityClass: KClass<SmsNotification> = SmsNotification::class

    override fun populateRecord(
        notification: SmsNotification,
        record: NotificationRecord,
        objectMapper: ObjectMapper,
    ) {
        record.writeCommonFields(notification, type, objectMapper)
        record.fromShortCode = notification.from.toInt()
        record.toPhone = notification.to.value
    }

    override fun fromRecord(
        record: NotificationRecord,
        objectMapper: ObjectMapper,
    ): Either<BaseError, SmsNotification> =
        either {
            val entityId = readEntityId(record)
            val fromShortCode = requireField(record.fromShortCode, "fromShortCode", TYPE).toUInt()
            val to = BrazilianPhone.create(requireField(record.toPhone, "toPhone", TYPE)).bind()
            val dates = record.readDates()

            SmsNotification(
                content = record.content,
                payload = record.deserializePayload(objectMapper),
                shippingReceipt = record.deserializeShippingReceipt(objectMapper),
                sentAt = dates.sentAt,
                deliveryAt = dates.deliveryAt,
                seenAt = dates.seenAt,
                id = entityId,
                createdAt = dates.createdAt,
                updatedAt = dates.updatedAt,
                from = fromShortCode,
                to = to,
                isNew = false,
            )
        }

    companion object {
        const val TYPE = "SMS"
    }
}
