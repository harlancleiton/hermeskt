package br.com.olympus.hermes.shared.config

import br.com.olympus.hermes.shared.infrastructure.persistence.NotificationRecord
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
    @ConfigProperty(name = "dynamodb.table-name") private lateinit var tableName: String

    @Produces
    @Singleton
    @NotificationTable
    fun notificationTable(
            enhancedClient: DynamoDbEnhancedClient
    ): DynamoDbTable<NotificationRecord> =
            enhancedClient.table(
                    tableName,
                    TableSchema.fromBean(NotificationRecord::class.java),
            )
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class NotificationTable
