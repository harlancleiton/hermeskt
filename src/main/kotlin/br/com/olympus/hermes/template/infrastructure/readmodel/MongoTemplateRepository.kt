package br.com.olympus.hermes.template.infrastructure.readmodel

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import br.com.olympus.hermes.shared.domain.core.NotificationType
import br.com.olympus.hermes.shared.domain.exceptions.BaseError
import br.com.olympus.hermes.shared.domain.exceptions.PersistenceError
import br.com.olympus.hermes.template.domain.entities.NotificationTemplate
import br.com.olympus.hermes.template.domain.repositories.TemplateRepository
import br.com.olympus.hermes.template.domain.valueobjects.TemplateBody
import br.com.olympus.hermes.template.domain.valueobjects.TemplateName
import br.com.olympus.hermes.template.infrastructure.config.TemplateMongo
import com.mongodb.client.MongoClient
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import io.quarkus.panache.common.Page
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Instant
import java.util.Date

@TemplateMongo
@ApplicationScoped
class MongoTemplateRepository(
    private val mongoClient: MongoClient,
    @ConfigProperty(name = "quarkus.mongodb.database") private val databaseName: String,
) : TemplateRepository {
    @PostConstruct
    fun ensureIndexes() {
        val collection = mongoClient.getDatabase(databaseName).getCollection("templates")
        collection.createIndex(
            Indexes.ascending("name", "channel"),
            IndexOptions().unique(true),
        )
    }

    override fun findByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, NotificationTemplate?> =
        Either
            .catch {
                TemplateDocument
                    .find("name = ?1 and channel = ?2", name.value, channel.name)
                    .firstResult()
            }.mapLeft { ex -> PersistenceError(ex.message ?: "Unknown error", ex) }
            .flatMap { doc ->
                if (doc == null) {
                    Either.Right(null)
                } else {
                    doc.toDomain().map { it }
                }
            }

    override fun findAllByChannel(
        channel: NotificationType?,
        page: Int,
        size: Int,
    ): Either<BaseError, List<NotificationTemplate>> =
        Either
            .catch {
                val query =
                    if (channel == null) {
                        TemplateDocument.findAll()
                    } else {
                        TemplateDocument.find("channel", channel.name)
                    }
                query.page(Page.of(page, size)).list()
            }.mapLeft { ex -> PersistenceError(ex.message ?: "Unknown error", ex) }
            .flatMap { docs ->
                docs.fold(
                    Either.Right(emptyList<NotificationTemplate>()) as
                        Either<BaseError, List<NotificationTemplate>>,
                ) { acc, doc ->
                    acc.flatMap { list ->
                        doc.toDomain().map { template -> list + template }
                    }
                }
            }

    override fun save(template: NotificationTemplate): Either<BaseError, NotificationTemplate> =
        Either
            .catch {
                val now = Instant.now()
                val doc = template.toDocument(createdAtFallback = now, updatedAt = now)
                doc.persist()
                doc
            }.mapLeft { ex -> PersistenceError(ex.message ?: "Unknown error", ex) }
            .flatMap { doc -> doc.toDomain() }

    override fun update(template: NotificationTemplate): Either<BaseError, NotificationTemplate> =
        Either
            .catch {
                val now = Instant.now()
                val doc =
                    template.toDocument(createdAtFallback = template.createdAt, updatedAt = now)
                doc.persistOrUpdate()
                doc
            }.mapLeft { ex -> PersistenceError(ex.message ?: "Unknown error", ex) }
            .flatMap { doc -> doc.toDomain() }

    override fun deleteByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, Boolean> =
        Either
            .catch {
                TemplateDocument.delete("name = ?1 and channel = ?2", name.value, channel.name) > 0
            }.mapLeft { ex -> PersistenceError(ex.message ?: "Unknown error", ex) }

    override fun existsByNameAndChannel(
        name: TemplateName,
        channel: NotificationType,
    ): Either<BaseError, Boolean> =
        Either
            .catch {
                TemplateDocument.count("name = ?1 and channel = ?2", name.value, channel.name) > 0
            }.mapLeft { ex -> PersistenceError(ex.message ?: "Unknown error", ex) }

    private fun TemplateDocument.toDomain(): Either<BaseError, NotificationTemplate> =
        Either
            .catch { NotificationType.valueOf(channel) }
            .mapLeft { ex -> PersistenceError(ex.message ?: "Unknown error", ex) }
            .flatMap { notificationType ->
                either {
                    val templateName =
                        TemplateName
                            .create(name)
                            .mapLeft { err -> PersistenceError(err.message, null) }
                            .bind()
                    val templateBody =
                        TemplateBody
                            .create(body)
                            .mapLeft { err -> PersistenceError(err.message, null) }
                            .bind()
                    NotificationTemplate(
                        name = templateName,
                        channel = notificationType,
                        subject = subject,
                        body = templateBody,
                        description = description,
                        createdAt = createdAt.toInstant(),
                        updatedAt = updatedAt.toInstant(),
                    )
                }
            }

    private fun NotificationTemplate.toDocument(
        createdAtFallback: java.time.Instant,
        updatedAt: java.time.Instant,
    ): TemplateDocument =
        TemplateDocument().also { doc ->
            doc.id = "${name.value}:${channel.name}"
            doc.name = name.value
            doc.channel = channel.name
            doc.subject = subject
            doc.body = body.value
            doc.description = description
            doc.createdAt = java.util.Date.from(createdAtFallback)
            doc.updatedAt = java.util.Date.from(updatedAt)
        }
}
