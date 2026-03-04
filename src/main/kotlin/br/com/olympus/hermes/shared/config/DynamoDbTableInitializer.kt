package br.com.olympus.hermes.shared.config

import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.eclipse.microprofile.config.inject.ConfigProperty
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*

@ApplicationScoped
class DynamoDbTableInitializer(
    private val dynamoDbClient: DynamoDbClient,
    @ConfigProperty(name = "quarkus.profile") private val profile: String,
    @ConfigProperty(name = "dynamodb.event-store-table-name") private val tableName: String,
) {
    fun onStart(
        @Observes ev: StartupEvent,
    ) {
        if (profile != "dev") {
            Log.info("Skipping DynamoDB table initialization (profile: $profile)")
            return
        }

        Log.info("Initializing DynamoDB tables for development environment...")
        createEventTableIfNotExists()
    }

    private fun createEventTableIfNotExists() {
        try {
            Log.info(dynamoDbClient.listTables())
            // Verifica se a tabela já existe
            val existingTables = dynamoDbClient.listTables().tableNames()
            if (existingTables.contains(tableName)) {
                Log.info("Table '$tableName' already exists, skipping creation")
                return
            }

            // Cria a tabela
            dynamoDbClient.createTable { builder ->
                builder
                    .tableName(tableName)
                    .keySchema(
                        KeySchemaElement
                            .builder()
                            .attributeName("PK")
                            .keyType(KeyType.HASH)
                            .build(),
                        KeySchemaElement
                            .builder()
                            .attributeName("SK")
                            .keyType(KeyType.RANGE)
                            .build(),
                    ).attributeDefinitions(
                        AttributeDefinition
                            .builder()
                            .attributeName("PK")
                            .attributeType(ScalarAttributeType.S)
                            .build(),
                        AttributeDefinition
                            .builder()
                            .attributeName("SK")
                            .attributeType(ScalarAttributeType.S)
                            .build(),
                    ).provisionedThroughput { throughput ->
                        throughput.readCapacityUnits(5L).writeCapacityUnits(5L)
                    }
            }

            // Aguarda a tabela estar ativa
            dynamoDbClient.waiter().waitUntilTableExists { it.tableName(tableName) }

            Log.info("Table '$tableName' created successfully with PK and SK")
        } catch (e: ResourceInUseException) {
            Log.info("Table '$tableName' already exists")
        } catch (e: Exception) {
            Log.error("Error creating table '$tableName'", e)
            throw e
        }
    }
}
