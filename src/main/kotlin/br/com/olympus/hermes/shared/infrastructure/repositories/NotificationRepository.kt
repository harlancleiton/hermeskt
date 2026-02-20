package br.com.olympus.hermes.shared.infrastructure.repositories

import br.com.olympus.hermes.shared.domain.entities.Notification
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema

@ApplicationScoped
class NotificationRepository @Inject constructor(enhancedClient: DynamoDbEnhancedClient) {

    private val table: DynamoDbTable<Notification> = enhancedClient.table(
        "notifications",
        TableSchema.fromBean(Notification::class.java)
    )

    fun save(notification: Notification): Notification {
        table.putItem(notification)
        return notification
    }

    fun findById(id: String, timestamp: String): Notification? {
        val key = Key.builder()
            .partitionValue(id)
            .sortValue(timestamp)
            .build()
        return table.getItem(key)
    }
}

