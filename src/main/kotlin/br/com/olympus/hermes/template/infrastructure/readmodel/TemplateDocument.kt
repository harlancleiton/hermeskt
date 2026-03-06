package br.com.olympus.hermes.template.infrastructure.readmodel

import io.quarkus.mongodb.panache.kotlin.PanacheMongoCompanion
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntityBase
import java.util.Date
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty

@io.quarkus.mongodb.panache.common.MongoEntity(collection = "templates")
class TemplateDocument : PanacheMongoEntityBase() {
    companion object : PanacheMongoCompanion<TemplateDocument>

    @BsonId var id: String = ""

    @BsonProperty("name") var name: String = ""

    @BsonProperty("channel") var channel: String = ""

    @BsonProperty("subject") var subject: String? = null

    @BsonProperty("body") var body: String = ""

    @BsonProperty("description") var description: String? = null

    @BsonProperty("createdAt") var createdAt: Date = Date()

    @BsonProperty("updatedAt") var updatedAt: Date = Date()
}
