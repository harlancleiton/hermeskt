package br.com.olympus.hermes.notification.infrastructure.persistence

import arrow.core.Either
import arrow.core.raise.either
import br.com.olympus.hermes.notification.domain.entities.PushNotification
import br.com.olympus.hermes.notification.domain.valueobjects.DeviceToken
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass

/** Converts [PushNotification] domain entities to/from [NotificationRecord] DynamoDB items. */
class PushNotificationRecordConverter : NotificationRecordConverter<PushNotification> {
    override val type: String = TYPE
    override val entityClass: KClass<PushNotification> = PushNotification::class

    override fun populateRecord(
        notification: PushNotification,
        record: NotificationRecord,
        objectMapper: ObjectMapper,
    ) {
        record.writeCommonFields(notification, type, objectMapper)
        record.deviceToken = notification.deviceToken.value
        record.title = notification.title
        record.data = objectMapper.writeValueAsString(notification.data)
    }

    override fun fromRecord(
        record: NotificationRecord,
        objectMapper: ObjectMapper,
    ): Either<BaseError, PushNotification> =
        either {
            val entityId = readEntityId(record)
            val deviceToken = DeviceToken.create(requireField(record.deviceToken, "deviceToken", TYPE)).bind()
            val title = requireField(record.title, "title", TYPE)
            val dataStr = record.data ?: "{}"
            val dataMap =
                Either
                    .catch {
                        objectMapper.readValue(dataStr, object : TypeReference<Map<String, String>>() {})
                    }.mapLeft {
                        br.com.olympus.hermes.shared.domain.exceptions
                            .PersistenceError("Failed to deserialize data map", it)
                    }.bind()
            val dates = record.readDates()

            PushNotification(
                content = record.content,
                payload = record.deserializePayload(objectMapper),
                shippingReceipt = record.deserializeShippingReceipt(objectMapper),
                sentAt = dates.sentAt,
                deliveryAt = dates.deliveryAt,
                seenAt = dates.seenAt,
                id = entityId,
                createdAt = dates.createdAt,
                updatedAt = dates.updatedAt,
                deviceToken = deviceToken,
                title = title,
                data = dataMap,
                isNew = false,
            )
        }

    companion object {
        const val TYPE = "PUSH"
    }
}
