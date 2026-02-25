package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import br.com.olympus.hermes.shared.domain.entities.Notification
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.domain.repositories.NotificationRepository
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import br.com.olympus.hermes.shared.infrastructure.config.NotificationTable
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key

/**
 * DynamoDB implementation of [NotificationRepository]. Persists notification aggregates as
 * [NotificationRecord] items in a single DynamoDB table. All type-specific mapping logic is
 * delegated to [NotificationRecordConverter] instances resolved via
 * [NotificationRecordConverterRegistry], keeping this class completely type-agnostic.
 */
@ApplicationScoped
class DynamoDbNotificationRepository
@Inject
constructor(
        @param:NotificationTable private val table: DynamoDbTable<NotificationRecord>,
        private val objectMapper: ObjectMapper,
        private val converterRegistry: NotificationRecordConverterRegistry
) : NotificationRepository {

    override fun save(notification: Notification): Either<BaseError, Notification> =
            converterRegistry.forEntity(notification).flatMap { converter ->
                Either.catch {
                    val record = NotificationRecord()
                    converter.populateRecord(notification, record, objectMapper)
                    table.putItem(record)
                    notification
                }
                        .mapLeft { PersistenceError(it.message ?: "Unknown error", it) }
            }

    override fun findById(id: EntityId): Either<BaseError, Notification?> = either {
        val key = Key.builder().partitionValue(id.value.toString()).build()
        val record =
                Either.catch { table.getItem(key) }
                        .mapLeft<BaseError> { PersistenceError(it.message ?: "Unknown error", it) }
                        .bind()
                        ?: return@either null

        val converter = converterRegistry.forType(record.type).bind()
        converter.fromRecord(record, objectMapper).bind()
    }
}
