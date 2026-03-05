# Test Strategy: Notification Delivery Pipeline

## 1. Test Strategy Overview

### Testing Scope

- `NotificationDeliveryFailedEvent` domain event
- `ProviderReceipt` value object
- `NotificationProviderAdapter` port interface
- `ProviderAdapterRegistry` resolution logic
- `DeliveryHandler` Kafka consumer (routing, idempotency, error handling)
- `NotificationSentEventHandler` projector
- `NotificationFailedEventHandler` projector
- `NotificationView.failureReason` field
- `KafkaEventWrapper` sent/failed event serialization
- Kafka channel configuration (consumer groups, DLQ)

### Quality Objectives

- ≥ 80 % line coverage for all new delivery pipeline code
- 100 % acceptance criteria validation
- Delivery latency < 5 s p95 (measured in integration tests)
- Zero duplicate deliveries under event replay

### Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Duplicate delivery on Kafka rebalance | Medium | High | Idempotency guard (check `sentAt` before sending) |
| DeliveryHandler and projector consume same event race | Medium | Medium | Separate consumer groups; delivery does not depend on view |
| Provider timeout blocks consumer thread | Medium | High | Configurable timeout; `@Blocking` annotation |
| DLQ fills up silently | Low | High | DLQ depth monitoring; structured logging on nack |
| Event reconstitution fails for corrupted events | Low | High | Graceful error handling; skip + log malformed events |

## 2. Test Design Techniques

### State Transition: Delivery Lifecycle

```
[Created] --DeliveryHandler success--> [Sent] --provider callback--> [Delivered]
[Created] --DeliveryHandler failure--> [Failed] --retry--> [Created] (re-consume)
[Created] --max retries--> [DLQ]
[Sent] --re-consume (idempotent)--> [Sent] (no-op)
```

### Decision Table: DeliveryHandler Routing

| Event Type | Aggregate reconstituted | sentAt | Adapter found | Provider result | Expected |
|:---:|:---:|:---:|:---:|:---:|:---|
| NotificationCreatedEvent | Yes | null | Yes | Success | markAsSent → publish SentEvent |
| NotificationCreatedEvent | Yes | null | Yes | Failure | publish FailedEvent → throw RuntimeException |
| NotificationCreatedEvent | Yes | non-null | — | — | Skip (idempotent), log info |
| NotificationCreatedEvent | Yes | null | No | — | ProviderAdapterNotFoundError → throw |
| NotificationCreatedEvent | No (reconstitute fails) | — | — | — | Log error → throw RuntimeException |
| Unknown event type | — | — | — | — | Skip + log warning |

## 3. Test Plan

### 3.1 Unit Tests

#### DeliveryHandlerTest
- `should invoke provider adapter for valid NotificationCreatedEvent`
- `should call markAsSent and persist sent event on success`
- `should publish NotificationSentEvent to Kafka on success`
- `should publish NotificationDeliveryFailedEvent on provider failure`
- `should throw RuntimeException on provider failure for nack`
- `should skip delivery when aggregate already has sentAt (idempotent)`
- `should throw RuntimeException when aggregate reconstitution fails`
- `should throw RuntimeException when provider adapter not found`
- `should skip and log warning for unknown event type`
- `should skip malformed JSON gracefully`

#### ProviderAdapterRegistryTest
- `should resolve adapter for EMAIL type`
- `should resolve adapter for SMS type`
- `should resolve adapter for PUSH type`
- `should resolve adapter for WHATSAPP type`
- `should return ProviderAdapterNotFoundError for unregistered type`

#### NotificationSentEventHandlerTest
- `should update NotificationView.sentAt on valid event`
- `should be idempotent — re-processing same event yields same state`

#### NotificationFailedEventHandlerTest
- `should update NotificationView.failureReason on valid event`
- `should be idempotent — re-processing same event yields same state`

### 3.2 Integration Tests

#### DeliveryHandlerIT (`@QuarkusTest`)
- `end-to-end: create notification → Kafka event → DeliveryHandler → provider called`
- `sent event published and projected into NotificationView.sentAt`
- `failed event published and projected into NotificationView.failureReason`
- `idempotency: replay same event does not call provider twice`

#### KafkaEventWrapperDeliveryIT
- `round-trip: NotificationSentEvent → KafkaEventWrapper → NotificationSentEvent`
- `round-trip: NotificationDeliveryFailedEvent → KafkaEventWrapper → NotificationDeliveryFailedEvent`

#### NotificationSentEventHandlerIT
- `consume sent event JSON from Kafka and verify NotificationView update`

#### NotificationFailedEventHandlerIT
- `consume failed event JSON from Kafka and verify NotificationView update`

### 3.3 Regression Tests
- `existing NotificationCreatedEventHandler projector still works alongside DeliveryHandler`
- `GET /notifications/{id} still returns correct view after delivery pipeline integration`

## 4. Quality Gates

### Entry Criteria
- Feature F4 (Provider Adapters) has at least one stub/mock adapter available for testing
- All domain events and ports defined
- Kafka topics configured in `application.properties`

### Exit Criteria
- All unit and integration tests pass
- ≥ 80 % line coverage
- No duplicate deliveries in replay tests
- DLQ messages contain meaningful error context
- Code review approved

## 5. Test Estimation

| Test Category | Estimated Effort |
|--------------|-----------------|
| Unit tests (DeliveryHandler, Registry, Projectors) | 2 SP |
| Integration tests (end-to-end delivery) | 2.5 SP |
| Integration tests (Kafka round-trip, projectors) | 1.5 SP |
| Regression tests | 0.5 SP |
| **Total** | **6.5 SP** |
