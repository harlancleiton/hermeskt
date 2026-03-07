# Test Strategy: f3-notification-dispatch-flow

## 1. Test Strategy Overview

**Testing Scope:**
The scope includes the entire CQRS command flow from the `CreateNotificationHandler` through to the Domain Aggregate (`Notification`), the output port dispatch (`NotificationProviderPort`), the DynamoDB Event Store persistence, the Kafka domain event publication (`NotificationCreated`), and the corresponding MongoDB Read Model projector (`NotificationCreatedProjector`).

**Quality Objectives:**
- Prove that a valid command results in exactly one Aggregate saved to DynamoDB.
- Prove that the aggregate correctly triggers the external provider port exactly once per creation.
- Prove that the read model accurately reflects the sent notification's payload and status.
- Ensure the failure of the external provider port is handled according to requirements (e.g., throwing a specific `ClientError` or `ServerError` leading to an `Either.Left` from the handler).

**Risk Assessment:**
- **Risk**: Kafka publisher fails to emit the event after the aggregate commits to DynamoDB.
  - **Mitigation**: Integration testing of the `DomainEventPublisher` with a real broker.
- **Risk**: The external provider port blocks indefinitely, hanging the handler.
  - **Mitigation**: Unit tests for the port adapter asserting timeouts (via Resilience4j configurations) throw appropriate errors within the expected timeframe.

## 2. ISTQB Framework Implementation

### Test Design Techniques Selection
- **State Transition Testing**: The notification goes from `Creation Intent` -> `Provider Dispatched` -> `Event Emitted` -> `Projected to Read Model`. We must test this full lifecycle.
- **Experience-Based Testing**: Simulating provider timeouts and HTTP 500s from the "external" email API to verify resilience policies.

### Test Types Coverage Matrix
- **Unit Testing**: 
  - `CreateNotificationHandler`: Mock the repository and provider port. Verify the `Either` return types on success and port failure.
  - `Notification` Aggregate: Verify the `NotificationCreated` event is correctly generated and placed in the uncommitted changes list.
  - `NotificationCreatedProjector`: Verify it correctly maps the domain event to a `NotificationView` Panache entity.
- **Integration Testing**: 
  - `QuarkusTest` pulling together DynamoDB (LocalStack or Testcontainers), MongoDB, and Kafka, firing the command and asserting the final view appears in MongoDB.

## 3. ISO 25010 Quality Characteristics Assessment

- **Functional Suitability (Critical)**: The core intent-to-send is registered and executed.
- **Reliability (High)**: Handled elegantly via Arrow-kt functional errors if the external provider is down.
- **Maintainability (High)**: Strictly following the Hexagonal isolation around the `NotificationProviderPort`.

## 4. Test Environment and Data Strategy

- **Test Environment Requirements**: 
  - `quarkus-test-dynamodb` / LocalStack container for the event store.
  - `quarkus-test-mongodb` for the read model.
  - `quarkus-test-kafka` / Redpanda for message transit.
- **Test Data Management**: `JavaFaker` to generate random recipients, template IDs, and payload maps.
- **Tool Selection**: `junit5`, `mockk`, `awaitility` (crucial for waiting on the Kafka to Mongo projection in integration tests), `rest-assured` (if checking an eventual REST controller, though out of scope for the internal handler itself).

---

## 5. Quality Assurance Plan / Quality Gates

**Entry Criteria (for PR Merging):**
- Implementation fully complete (Handler, Aggregate, Repository, Provider Adapter, Projector, View).
- Unit tests written for all components with > 90% branch coverage.
- Full E2E integration test `@QuarkusTest` written and passing locally.

**Exit Criteria:**
- CI pipeline passes successfully (meaning containers spin up and test succeeds).
- External provider port has explicit retry/timeout configuration proven by tests.
