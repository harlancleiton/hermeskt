package br.com.olympus.hermes.shared.infrastructure.readmodel

import io.quarkus.mongodb.panache.kotlin.PanacheMongoCompanion
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntityBase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty
import java.util.Date

/**
 * MongoDB read-model document representing a denormalised, flat projection of a notification
 * aggregate. Optimised for fast reads — contains no business logic or domain rules. Projected and
 * kept up-to-date by event handlers listening to Kafka topics.
 */
@io.quarkus.mongodb.panache.common.MongoEntity(collection = "notifications")
class NotificationView : PanacheMongoEntityBase() {
    companion object : PanacheMongoCompanion<NotificationView>

    /** Aggregate identifier, used as the MongoDB document _id. */
    @BsonId var id: String = ""

    /** Notification channel type (EMAIL, SMS, WHATSAPP). */
    @BsonProperty("type")
    var type: String = ""

    /** Body content of the notification. */
    @BsonProperty("content")
    var content: String = ""

    /** Additional metadata payload. */
    @BsonProperty("payload")
    var payload: Map<String, Any> = emptyMap()

    /** Sender address — email address, phone number, or channel-specific identifier. */
    @BsonProperty("from")
    var from: String = ""

    /** Recipient address — email address, phone number, or channel-specific identifier. */
    @BsonProperty("to")
    var to: String = ""

    /** Email subject line. Null for non-email notifications. */
    @BsonProperty("subject")
    var subject: String? = null

    /** WhatsApp template name. Null for non-WhatsApp notifications. */
    @BsonProperty("templateName")
    var templateName: String? = null

    /** Device token for Push notifications. Null for other generic notifications. */
    @BsonProperty("deviceToken")
    var deviceToken: String? = null

    /** Push notification title. Null for other generic notifications. */
    @BsonProperty("title")
    var title: String? = null

    /** Timestamp when the notification was created. */
    @BsonProperty("createdAt")
    var createdAt: Date = Date()

    /** Timestamp when the notification was last updated. */
    @BsonProperty("updatedAt")
    var updatedAt: Date = Date()

    /** Timestamp when the notification was sent. Null if not yet sent. */
    @BsonProperty("sentAt")
    var sentAt: Date? = null

    /** Timestamp when the notification was delivered. Null if not yet delivered. */
    @BsonProperty("deliveryAt")
    var deliveryAt: Date? = null

    /** Timestamp when the notification was seen by the recipient. Null if not yet seen. */
    @BsonProperty("seenAt")
    var seenAt: Date? = null
}
