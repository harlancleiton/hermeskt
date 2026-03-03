package br.com.olympus.hermes.shared.infrastructure.persistence

import io.quarkus.runtime.annotations.RegisterForReflection
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import kotlin.text.appendLine

/**
 * DynamoDB persistence model for notification aggregates. Uses a single-table design with a `type`
 * discriminator to support polymorphic notifications (Email, SMS). Channel-specific fields are
 * nullable and populated based on the notification type.
 */
@DynamoDbBean
@RegisterForReflection
class NotificationRecord {
    @get:DynamoDbPartitionKey
    @get:DynamoDbAttribute("PK")
    var pk: String = ""

    @get:DynamoDbSortKey
    @get:DynamoDbAttribute("SK")
    var sk: String = ""

    val id
        get() = pk

    @get:DynamoDbAttribute("type")
    var type: String = ""

    @get:DynamoDbAttribute("content")
    var content: String = ""

    @get:DynamoDbAttribute("payload")
    var payload: String = "{}"

    @get:DynamoDbAttribute("shippingReceipt")
    var shippingReceipt: String? = null

    @get:DynamoDbAttribute("sentAt")
    var sentAt: Long? = null
    var deliveryAt: Long? = null

    @get:DynamoDbAttribute("seenAt")
    var seenAt: Long? = null

    @get:DynamoDbAttribute("createdAt")
    var createdAt: Long = 0

    @get:DynamoDbAttribute("updatedAt")
    var updatedAt: Long = 0

    @get:DynamoDbAttribute("version")
    var version: Int = 0

    // Email-specific
    @get:DynamoDbAttribute("fromEmail")
    var fromEmail: String? = null

    @get:DynamoDbAttribute("toEmail")
    var toEmail: String? = null

    @get:DynamoDbAttribute("subject")
    var subject: String? = null

    // SMS-specific
    @get:DynamoDbAttribute("fromShortCode")
    var fromShortCode: Int? = null

    @get:DynamoDbAttribute("toPhone")
    var toPhone: String? = null

    // WhatsApp-specific
    @get:DynamoDbAttribute("fromWhatsApp")
    var fromWhatsApp: String? = null

    @get:DynamoDbAttribute("toWhatsApp")
    var toWhatsApp: String? = null

    @get:DynamoDbAttribute("templateName")
    var templateName: String? = null

    override fun toString(): String =
        StringBuilder()
            .appendLine("pk: $pk")
            .appendLine("sk: $sk")
            .appendLine("id: $id")
            .appendLine("type: $type")
            .appendLine("content: $content")
            .appendLine("payload: $payload")
            .appendLine("shippingReceipt: $shippingReceipt")
            .appendLine("sentAt: $sentAt")
            .appendLine("deliveryAt: $deliveryAt")
            .appendLine("seenAt: $seenAt")
            .appendLine("createdAt: $createdAt")
            .appendLine("updatedAt: $updatedAt")
            .appendLine("version: $version")
            .appendLine("fromEmail: $fromEmail")
            .appendLine("toEmail: $toEmail")
            .appendLine("subject: $subject")
            .appendLine("fromShortCode: $fromShortCode")
            .appendLine("toPhone: $toPhone")
            .appendLine("fromWhatsApp: $fromWhatsApp")
            .appendLine("toWhatsApp: $toWhatsApp")
            .appendLine("templateName: $templateName")
            .toString()
}
