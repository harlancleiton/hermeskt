package br.com.olympus.hermes.shared.infrastructure.persistence

import io.quarkus.runtime.annotations.RegisterForReflection
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey

/**
 * DynamoDB persistence model for domain events. Each item represents a single event in an
 * aggregate's event stream.
 *
 * Key schema:
 * - **PK** = aggregate ID (partition key)
 * - **SK** = zero-padded aggregate version, e.g. `0000000001` (sort key, ensures chronological
 * ordering)
 */
@DynamoDbBean
@RegisterForReflection
class EventRecord {
    @get:DynamoDbPartitionKey
    @get:DynamoDbAttribute("PK")
    var pk: String = ""

    @get:DynamoDbSortKey
    @get:DynamoDbAttribute("SK")
    var sk: String = ""

    /** Unique event identifier. */
    var eventId: String = ""

    /** Discriminator string identifying the concrete [DomainEvent] subtype. */
    var eventType: String = ""

    /** The aggregate type, e.g. "Notification". */
    var aggregateType: String = ""

    /** JSON-serialized event-specific payload. */
    var data: String = "{}"

    /** Epoch-millisecond timestamp of when the event occurred. */
    var occurredAt: Long = 0

    /** The aggregate version this event corresponds to. */
    var version: Int = 0

    companion object {
        private const val SK_PAD_LENGTH = 10

        /** Formats a version integer into a zero-padded sort key string. */
        fun sortKey(version: Int): String = version.toString().padStart(SK_PAD_LENGTH, '0')

        /** Parses a sort key string back into a version integer. */
        fun versionFromSortKey(sk: String): Int = sk.trimStart('0').ifEmpty { "0" }.toInt()
    }
}
