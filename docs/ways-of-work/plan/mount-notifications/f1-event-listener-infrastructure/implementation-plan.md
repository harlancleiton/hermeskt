# Feature Implementation Plan: f1-event-listener-infrastructure

## Goal
Implement the foundational Kafka consumer infrastructure within the Hermes `notification` context. This will provide a reliable, at-least-once delivery mechanism to ingest domain events emitted by upstream contexts and route them for further processing.

## Requirements
- Define an `@Incoming` SmallRye Kafka consumer in the `infrastructure/messaging/consumers` package.
- Accept standard payloads (via `KafkaEventWrapper` or generic JSON).
- Process messages using `@Blocking` to allow for downstream blocking I/O.
- Implement a Dead Letter Queue (DLQ) failure strategy.
- Configure application properties for the Kafka topic, consumer group, and DLQ.
- Include OpenTelemetry tracing (`@WithSpan`).

## Technical Considerations

### System Architecture Overview

```mermaid
graph TD
    subgraph Kafka Broker
        TopicMain[Topic: hermes.domain.events]
        TopicDLQ[Topic: hermes.domain.events.dlq]
    end

    subgraph Hermes System
        subgraph Notification Context
            subgraph Infrastructure Layer
                Consumer[GenericEventConsumer<br/>@Incoming<br/>@Blocking]
                Deserializer[Jackson Deserializer]
            end
            
            subgraph Application Layer
                EventRouter[EventRouter / Handler Interface]
            end
        end
    end

    TopicMain -- JSON Payload --> Consumer
    Consumer -- Parses --> Deserializer
    Deserializer -- Valid/Mapped --> EventRouter
    Consumer -. Exception/Failure .-> TopicDLQ
```

- **Technology Stack Selection**: 
  - **Quarkus SmallRye Reactive Messaging**: Standard mechanism in the Hermes project for Kafka integration.
  - **Jackson**: For JSON deserialization of the incoming payload.
- **Integration Points**: Listens to an external Kafka broker. Passes validated events to the internal application layer.
- **Deployment Architecture**: Runs within the standard Quarkus JVM/Native Docker container.

### Database Schema Design
*Not applicable for this feature as it purely deals with messaging infrastructure. No new database tables or collections are introduced.*

### API Design
*Not applicable. This feature does not expose REST endpoints.*

**Messaging Contract (Internal):**
Incoming messages are expected to be JSON. The consumer will likely ingest them as `JsonObject` or a generic `Map<String, Any?>` representing the `KafkaEventWrapper` before attempting to route them to specific typed handlers in f2/f3.

### Frontend Architecture
*Not applicable. Backend infrastructure only.*

### Security & Performance
- **Security**: Kafka connections should use SSL/SASL in production (configured via standard Quarkus properties, out of scope for the code implementation itself, but supported by the configuration).
- **Performance**: 
  - The `@Blocking` annotation is critical to ensure the reactive event loop is not blocked by downstream database operations.
  - SmallRye's default concurrency and backpressure mechanics will handle flow control.
- **Resilience**: The DLQ configuration (`failure-strategy=dead-letter-queue`) ensures poison pill messages do not halt the consumer group. Unhandled exceptions in the consumer method automatically trigger this routing.
