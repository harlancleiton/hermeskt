package br.com.olympus.hermes.shared.config

import br.com.olympus.hermes.shared.infrastructure.persistence.DomainEventSerde
import br.com.olympus.hermes.shared.infrastructure.persistence.EventRecord
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Qualifier
import jakarta.inject.Singleton
import jakarta.ws.rs.Produces
import org.eclipse.microprofile.config.inject.ConfigProperty
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

/**
 * CDI producer for the DynamoDB Enhanced Client. Wraps the base [DynamoDbClient] (auto-configured
 * by Quarkus Amazon Services) into a [DynamoDbEnhancedClient] for higher-level table operations.
 */
@ApplicationScoped
class DynamoDbConfig {
    @ConfigProperty(name = "dynamodb.event-store-table-name")
    private lateinit var eventStoreTableName: String

    @Produces
    @Singleton
    @EventStoreTable
    fun eventStoreTable(enhancedClient: DynamoDbEnhancedClient): DynamoDbTable<EventRecord> =
        enhancedClient.table(
            eventStoreTableName,
            TableSchema.fromBean(EventRecord::class.java),
        )

    @Produces
    @Singleton
    fun domainEventSerde(objectMapper: ObjectMapper): DomainEventSerde = DomainEventSerde(objectMapper)
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class EventStoreTable
