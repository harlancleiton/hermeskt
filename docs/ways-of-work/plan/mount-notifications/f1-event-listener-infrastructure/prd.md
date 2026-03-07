# Feature PRD: f1-event-listener-infrastructure

## 1. Feature Name
f1-event-listener-infrastructure

## 2. Epic
[mount-notifications](../epic.md)  
[Architecture](../arch.md)

## 3. Goal
**Problem:** To build an event-driven notification system, we first need a reliable way to ingest the raw domain events emitted by other bounded contexts over Apache Kafka.
**Solution:** Implement the foundational Kafka consumer infrastructure within the Hermes `notification` context using SmallRye Reactive Messaging. This includes setting up the `@Incoming` channels, deserialization wrappers, and Dead Letter Queue (DLQ) routing.
**Impact:** This establishes the entry point for all upstream events, ensuring messages are reliably received and preventing data loss through robust failure strategies.

## 4. User Personas
- **System Administrator / Operator:** Needs visibility into consumer health and DLQ metrics.
- **Upstream Domain Developers:** Need a reliable contract and endpoint to send their events to. (Implicit persona).

## 5. User Stories
- As a Hermes system operator, I want the application to consume messages from configured Kafka topics so that upstream domain events can be processed.
- As a Hermes system operator, I want malformed or unprocessable messages to be routed to a Dead Letter Queue (DLQ) so that they don't block the consumer group and can be analyzed later.
- As a Hermes system operator, I want consumer activity to be observable via traces so that I can monitor latency and throughput.

## 6. Requirements

**Functional Requirements:**
- The system must define an `@Incoming` SmallRye Kafka consumer listening to a configurable channel (e.g., `mp.messaging.incoming.hermes-domain-events`).
- The consumer must accept standard domain event payloads (e.g., using `KafkaEventWrapper` or generic JSON deserialization aligned with the project's payload structure).
- The consumer must process messages using the `@Blocking` annotation since subsequent mounting/persistence steps will involve blocking I/O (MongoDB/DynamoDB).
- The consumer must catch local deserialization errors gracefully without silently discarding them, forcing a negative acknowledgement (nack) or DLQ routing as per configuration.

**Non-Functional Requirements:**
- **Reliability:** Must guarantee at-least-once configuration via Kafka consumer offsets.
- **Resilience:** The SmallRye configuration must explicitly define a Dead Letter Queue (DLQ) failure strategy (`mp.messaging.incoming.<channel>.failure-strategy=dead-letter-queue`).
- **Observability:** Must include OpenTelemetry tracing annotations (`@WithSpan`) to start a trace context when a message is received.

## 7. Acceptance Criteria
- [ ] Appropriate properties exist in `application.properties` configuring the incoming Kafka channel, bootstrap servers, consumer group, and DLQ failure strategy.
- [ ] A Kafka consumer class is implemented in the `infrastructure/messaging/consumers` package with an `@Incoming` method.
- [ ] The `@Incoming` method is marked as `@Blocking`.
- [ ] The `@Incoming` method extracts the payload and routes it to an application-layer service (to be implemented in f2/f3).
- [ ] Unit/Integration tests verify that valid JSON messages invoke the downstream application service.
- [ ] Unit/Integration tests verify that DLQ mechanics trigger on unhandled exceptions from the handler.

## 8. Out of Scope
- The actual parsing of templates or processing of the event payload (business logic). This is covered in `f2-template-mounting-engine`.
- The dispatch of the finalized notification to external providers. Covered in `f3-notification-dispatch-flow`.
