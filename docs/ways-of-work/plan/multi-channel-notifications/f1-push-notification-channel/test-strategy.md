# Test Strategy: Push Notification Channel

## 1. Test Strategy Overview

### Testing Scope

All new components introduced by the Push Notification channel feature:

- `DeviceToken` value object
- `PushNotification` entity
- `PushNotificationCreatedEvent` domain event
- `PushNotificationFactory` (create + reconstitute)
- `CreateNotificationCommand.Push` + `toInput()` mapping
- `CreatePushNotificationRequest` DTO + `toCommand()`
- `NotificationController.createPushNotification()` REST endpoint
- `NotificationResponse.from()` push branch
- `NotificationCreatedEventHandler.toView()` push branch
- `KafkaEventWrapper` push serialization/deserialization
- `NotificationView` push-specific fields
- `NotificationFactoryRegistry` push factory registration

### Quality Objectives

- ≥ 80 % line coverage for all new domain and application code
- 100 % acceptance criteria validation
- Zero regressions on existing Email, SMS, WhatsApp channels

### Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| `DeviceToken` regex too strict/loose | Medium | Medium | Boundary value tests with real-world FCM/APNs tokens |
| `NotificationFactoryRegistry` breaks existing channels after push registration | Low | High | Regression tests for all existing channels |
| `KafkaEventWrapper` deserialization mismatch | Medium | High | Round-trip serialization tests |
| `NotificationView` push fields break existing queries | Low | Medium | Backward-compatibility test: existing views still readable |

### Test Approach

- **Unit tests**: MockK for isolating domain/application components
- **Integration tests**: `@QuarkusTest` + REST-Assured for REST endpoints, real MongoDB (dev services) for projector tests
- **Test data**: JavaFaker for generating realistic device tokens, titles, and payloads

## 2. Test Design Techniques (ISTQB)

### Equivalence Partitioning

| Input | Valid Partition | Invalid Partitions |
|-------|---------------|-------------------|
| `deviceToken` | Non-blank string ≤ 4096 chars | Blank / empty string; String > 4096 chars |
| `title` | Non-blank string ≤ 255 chars | Blank / empty string; String > 255 chars |
| `body` (content) | Non-blank string | Blank / empty string |
| `payload` | Any valid `Map<String, Any>` | (always valid — defaults to empty) |
| `data` | Any valid `Map<String, String>` | (always valid — defaults to empty) |

### Boundary Value Analysis

| Boundary | Test Values |
|----------|------------|
| `deviceToken` length | 1 char (min valid), 4096 chars (max valid), 4097 chars (invalid) |
| `title` length | 1 char (min valid), 255 chars (max valid), 256 chars (invalid) |
| `deviceToken` blank | `""`, `"   "` (whitespace-only) |

### Decision Table: Factory Validation

| content blank | deviceToken invalid | title blank | Expected Result |
|:---:|:---:|:---:|:---|
| N | N | N | Success — `PushNotification` created |
| Y | N | N | `ValidationErrors` with `EmptyContentError` |
| N | Y | N | `ValidationErrors` with `InvalidDeviceTokenError` |
| N | N | Y | `ValidationErrors` with `EmptyContentError("title")` |
| Y | Y | Y | `ValidationErrors` with 3 accumulated errors |

### State Transition: Aggregate Lifecycle

```
[New] --applyChange(PushNotificationCreatedEvent)--> [Created]
[Created] --markAsSent()--> [Sent]
[Sent] --markAsDelivered()--> [Delivered]
```

## 3. Test Plan

### 3.1 Unit Tests

#### DeviceTokenTest

- `should create valid DeviceToken from non-blank string`
- `should return InvalidDeviceTokenError for blank string`
- `should return InvalidDeviceTokenError for whitespace-only string`
- `should return InvalidDeviceTokenError for string exceeding 4096 chars`
- `should create DeviceToken with exactly 4096 chars`
- `should trim whitespace from valid token`

#### PushNotificationFactoryTest

- `create should return PushNotification for valid input`
- `create should accumulate errors for all invalid fields`
- `create should return InvalidNotificationInputError for wrong input type`
- `reconstitute should rebuild aggregate from event history`
- `reconstitute should return InvalidEventHistoryError for empty events`
- `reconstitute should return MissingCreationEventError when creation event missing`

#### PushNotificationTest

- `should raise PushNotificationCreatedEvent when isNew is true`
- `should NOT raise event when isNew is false`
- `should contain correct fields in created event`

#### CreateNotificationCommandPushTest

- `toInput should map all fields correctly`

#### NotificationResponsePushTest

- `from should return PUSH type for PushNotification`

### 3.2 Integration Tests

#### NotificationControllerPushIT (`@QuarkusTest`)

- `POST /notifications/push with valid request returns 201`
- `POST /notifications/push with blank deviceToken returns 400`
- `POST /notifications/push with blank title returns 400`
- `POST /notifications/push with blank body returns 400`
- `POST /notifications/push with multiple invalid fields returns 400 with accumulated errors`

#### NotificationCreatedEventHandlerPushIT

- `consume should project PushNotificationCreatedEvent into NotificationView with type PUSH`
- `consume should set deviceToken and title in NotificationView`
- `consume should be idempotent — re-processing same event yields same view state`

#### KafkaEventWrapperPushIT

- `round-trip serialization: PushNotificationCreatedEvent → KafkaEventWrapper → PushNotificationCreatedEvent`
- `toNotificationCreatedEvent returns null for unknown event types (no regression)`

### 3.3 Regression Tests

- `existing Email notification creation still works after push factory registration`
- `existing SMS notification creation still works`
- `existing WhatsApp notification creation still works`
- `GET /notifications/{id} returns existing email/sms/whatsapp views unchanged`

## 4. Quality Gates

### Entry Criteria

- All implementation tasks from the implementation plan completed
- Code compiles without errors (`./mvnw compile`)
- No new Kotlin warnings introduced

### Exit Criteria

- All unit tests pass
- All integration tests pass
- ≥ 80 % line coverage on new code
- No critical/high severity defects
- Code review approved
- OpenAPI spec includes `/notifications/push` endpoint

## 5. Test Estimation

| Test Category | Estimated Effort |
|--------------|-----------------|
| Unit tests (DeviceToken, Factory, Entity, Command, Response) | 1 SP |
| Integration tests (REST endpoint) | 1 SP |
| Integration tests (Projector + Kafka round-trip) | 1 SP |
| Regression tests | 0.5 SP |
| **Total** | **3.5 SP** |
