# Test Strategy: f1-event-listener-infrastructure

## 1. Test Strategy Overview

**Testing Scope:**
The scope includes the Kafka consumer component (`@Incoming` method), its integration with SmallRye Reactive Messaging, payload deserialization, and Dead Letter Queue (DLQ) routing. We aim to prove that the infrastructure successfully ingests valid messages and properly handles failures according to the defined retry and DLQ strategies.

**Quality Objectives:**
- Zero message loss during ingestion.
- 100% of malformed payloads or unhandled business exceptions correctly routed to the DLQ.
- Correct OpenTelemetry trace initialization on message consumption.

**Risk Assessment:**
- **Risk**: Deserialization exceptions are caught silently by Jackson, preventing the message from being nacked.
  - **Mitigation**: Integration testing with invalid JSON to verify DLQ routing.
- **Risk**: Blocking operations inside the consumer cause the reactive event loop to hang.
  - **Mitigation**: Ensure `@Blocking` annotation is present and tested under load/concurrency.

## 2. ISTQB Framework Implementation

### Test Design Techniques Selection
- **Equivalence Partitioning**: 
  - Valid JSON payloads representing registered domain events.
  - Invalid JSON payloads (malformed strings).
  - Valid JSON but unknown event types.
- **Experience-Based Testing**: Error guessing around Kafka disconnects and rebalances (tested via Testcontainers).

### Test Types Coverage Matrix
- **Integration Testing**: Using Quarkus `@QuarkusTest` combined with Redpanda/Kafka Testcontainers to verify end-to-end consumption from a real broker.
- **Structural Testing**: Minimum 90% branch coverage on the consumer class to ensure all `try/catch` paths (if any are introduced beyond the framework's defaults) are evaluated.

## 3. ISO 25010 Quality Characteristics Assessment

- **Functional Suitability (High)**: Does it consume the message?
- **Reliability (Critical)**: Does it recover from broker disconnections? Does it nack failed messages?
- **Maintainability (Medium)**: Is the consumer logic thin (delegating to application services)?

## 4. Test Environment and Data Strategy

- **Test Environment Requirements**: Quarkus Dev Services for Kafka (automatically spins up a Redpanda container during `./mvnw test`).
- **Test Data Management**: Use `JavaFaker` to generate random but valid JSON payloads for testing high-throughput consumption.
- **Tool Selection**: `quarkus-junit5`, `awaitility` (for async assertions on Kafka consumption), `KafkaCompanion` (for injecting messages into topics during tests).

---

## 5. Quality Assurance Plan / Quality Gates

**Entry Criteria (for PR Merging):**
- Implementation complete according to architecture rules.
- `@QuarkusTest` integration tests written and passing locally.

**Exit Criteria:**
- CI pipeline passes successfully.
- SonarQube/Code Coverage reports -> Branch coverage > 90% for the new consumer classes.
- DLQ behavior is explicitly verified by an automated test.
