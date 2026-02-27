package br.com.olympus.hermes.shared.infrastructure.persistence

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import br.com.olympus.hermes.shared.config.EventStoreTable
import br.com.olympus.hermes.shared.domain.events.DomainEvent
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.shared.domain.repositories.EventStore
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import software.amazon.awssdk.services.dynamodb.model.Put
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest

/**
 * DynamoDB implementation of [EventStore]. Persists domain events as individual items in a DynamoDB
 * table using:
 * - **PK** = aggregate ID
 * - **SK** = zero-padded version (ensures chronological ordering)
 *
 * Uses DynamoDB transactions with condition expressions for optimistic concurrency control.
 */
@ApplicationScoped
class DynamoDbEventStore
@Inject
constructor(
        @param:EventStoreTable private val table: DynamoDbTable<EventRecord>,
        private val dynamoDbClient: DynamoDbClient,
        private val serde: DomainEventSerde
) : EventStore {

    override fun append(
            aggregateId: EntityId,
            events: List<DomainEvent>,
            expectedVersion: Int
    ): Either<BaseError, Unit> {
        if (events.isEmpty()) return Unit.right()

        val tableName = table.tableName()
        val transactItems =
                events.mapIndexed { index, event ->
                    val version = expectedVersion + index
                    val record = toRecord(aggregateId, event, version)
                    TransactWriteItem.builder()
                            .put(
                                    Put.builder()
                                            .tableName(tableName)
                                            .item(toAttributeMap(record))
                                            .conditionExpression(
                                                    "attribute_not_exists(PK) AND attribute_not_exists(SK)"
                                            )
                                            .build()
                            )
                            .build()
                }

        return Either.catch {
            dynamoDbClient.transactWriteItems(
                    TransactWriteItemsRequest.builder().transactItems(transactItems).build()
            )
        }
                .map {}
                .mapLeft { ex ->
                    when (ex) {
                        is ConditionalCheckFailedException ->
                                PersistenceError(
                                        "Optimistic concurrency conflict for aggregate ${aggregateId.value} at version $expectedVersion",
                                        ex
                                )
                        else -> PersistenceError(ex.message ?: "Unknown error", ex)
                    }
                }
    }

    override fun getEvents(aggregateId: EntityId): Either<BaseError, List<DomainEvent>> {
        val condition =
                QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(aggregateId.value.toString()).build()
                )
        return queryAndDeserialize(condition)
    }

    override fun getEvents(
            aggregateId: EntityId,
            afterVersion: Int
    ): Either<BaseError, List<DomainEvent>> {
        val condition =
                QueryConditional.sortGreaterThan(
                        Key.builder()
                                .partitionValue(aggregateId.value.toString())
                                .sortValue(EventRecord.sortKey(afterVersion))
                                .build()
                )
        return queryAndDeserialize(condition)
    }

    // ========================================
    // Internal helpers
    // ========================================

    private fun queryAndDeserialize(
            condition: QueryConditional
    ): Either<BaseError, List<DomainEvent>> = either {
        val records =
                Either.catch { table.query(condition).items().toList() }
                        .mapLeft<BaseError> { PersistenceError(it.message ?: "Unknown error", it) }
                        .bind()

        records.map { record -> fromRecord(record).bind() }
    }

    private fun toRecord(aggregateId: EntityId, event: DomainEvent, version: Int): EventRecord {
        val record = EventRecord()
        record.pk = aggregateId.value.toString()
        record.sk = EventRecord.sortKey(version)
        record.eventId = event.id.value.toString()
        record.eventType = event.eventType
        record.aggregateType = event.aggregateType
        record.data = serde.serialize(event)
        record.occurredAt = event.occurredAt.time
        record.version = version
        return record
    }

    private fun fromRecord(record: EventRecord): Either<BaseError, DomainEvent> = either {
        val eventId = EntityId.from(record.eventId).bind()
        val aggregateId = EntityId.from(record.pk).bind()
        serde.deserialize(
                        eventType = record.eventType,
                        eventId = eventId,
                        aggregateId = aggregateId,
                        version = record.version,
                        occurredAt = Date(record.occurredAt),
                        json = record.data,
                )
                .bind()
    }

    /**
     * Converts an [EventRecord] bean into a raw DynamoDB attribute map for use in transact write
     * requests.
     */
    private fun toAttributeMap(record: EventRecord): Map<String, AttributeValue> =
            mapOf(
                    "PK" to AttributeValue.fromS(record.pk),
                    "SK" to AttributeValue.fromS(record.sk),
                    "eventId" to AttributeValue.fromS(record.eventId),
                    "eventType" to AttributeValue.fromS(record.eventType),
                    "aggregateType" to AttributeValue.fromS(record.aggregateType),
                    "data" to AttributeValue.fromS(record.data),
                    "occurredAt" to AttributeValue.fromN(record.occurredAt.toString()),
                    "version" to AttributeValue.fromN(record.version.toString()),
            )
}
