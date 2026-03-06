// Path: src/main/kotlin/br/com/olympus/hermes/shared/config/MongoIndexInitializer.kt
// Phase: Phase 1: Read Model Updates
// Covers: FR-8

package br.com.olympus.hermes.shared.config

import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.bson.Document

/** Initializes MongoDB indexes on application startup. */
@ApplicationScoped
class MongoIndexInitializer {
    /** Ensures necessary indexes are created. */
    fun onStart(
        @Observes ev: StartupEvent,
    ) {
        val collection = NotificationView.mongoCollection()

        // Compound index on type, status, and createdAt (descending)
        val indexKeys = Document("type", 1).append("status", 1).append("createdAt", -1)
        collection.createIndex(indexKeys)
    }
}
