package br.com.olympus.hermes.shared.infrastructure.persistence

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

/**
 * DynamoDB persistence model for notification aggregates. Uses a single-table design with a `type`
 * discriminator to support polymorphic notifications (Email, SMS). Channel-specific fields are
 * nullable and populated based on the notification type.
 */
@DynamoDbBean
class NotificationRecord {
    @get:DynamoDbPartitionKey var id: String = ""

    var type: String = ""
    var content: String = ""
    var payload: String = "{}"
    var shippingReceipt: String? = null
    var sentAt: Long? = null
    var deliveryAt: Long? = null
    var seenAt: Long? = null
    var createdAt: Long = 0
    var updatedAt: Long = 0
    var version: Int = 0

    // Email-specific
    var fromEmail: String? = null
    var toEmail: String? = null
    var subject: String? = null

    // SMS-specific
    var fromShortCode: Int? = null
    var toPhone: String? = null
}
