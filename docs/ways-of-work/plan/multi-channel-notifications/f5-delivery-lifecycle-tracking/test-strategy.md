# Test Strategy: Delivery Lifecycle Tracking

## 1. Test Strategy Overview

### Testing Scope

- `NotificationView` new fields (`status`, `failureReason`)
- `NotificationSentEventHandler` projector
- `NotificationFailedEventHandler` projector
- `NotificationCreatedEventHandler` — initial `status = PENDING`
- `NotificationViewRepository` new query methods (`findAll`, `countAll`, `updateStatus`)
- `ListNotificationsQuery` + `ListNotificationsQueryHandler`
- `GET /notifications` list endpoint (pagination, filtering)
- `GET /notifications/{id}` enhanced response with lifecycle fields
- `PaginatedNotificationResponse` and `NotificationViewResponse` DTOs
- MongoDB compound index `(type, status, createdAt)`
- `MongoIndexInitializer`

### Quality Objectives

- ≥ 80 % line coverage for all new code
- 100 % acceptance criteria validation
- List query < 100 ms p99 for 20-doc pages
- Projectors are idempotent

### Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Missing MongoDB index degrades list performance | Medium | High | Index existence assertion in integration test |
| Status field out of sync with lifecycle events | Medium | High | Projector idempotency tests; verify status matches timestamps |
| Pagination off-by-one errors | Medium | Low | Boundary tests: empty, 1 item, exact page boundary |
| Date range filter timezone mismatch | Medium | Medium | Use UTC consistently; test with explicit timezone offsets |

## 2. Test Design Techniques

### Equivalence Partitioning

| Filter Param | Valid Partitions | Invalid Partitions |
|-------------|-----------------|-------------------|
| `type` | EMAIL, SMS, PUSH, WHATSAPP, null (all) | Unknown string |
| `status` | PENDING, SENT, DELIVERED, FAILED, null (all) | Unknown string |
| `page` | 0, 1, N | Negative |
| `size` | 1–100 | 0, > 100, negative |
| `createdFrom` / `createdTo` | Valid ISO date, null | Malformed date string |

### Boundary Value Analysis

| Boundary | Test Values |
|----------|------------|
| `page` | 0 (first), last page (total/size), beyond last page (empty) |
| `size` | 1 (min), 100 (max), 101 (rejected) |
| Date range | `createdFrom = createdTo` (single day), inverted range (from > to) |
| Total elements | 0 (empty), 1, exact multiple of page size, non-multiple |

### Decision Table: Status Derivation

| sentAt | deliveryAt | failureReason | Expected Status |
|:---:|:---:|:---:|:---|
| null | null | null | PENDING |
| set | null | null | SENT |
| set | set | null | DELIVERED |
| null | null | set | FAILED |

## 3. Test Plan

### 3.1 Unit Tests

#### ListNotificationsQueryHandlerTest
- `should delegate to repository with all filter params`
- `should return empty paginated result when no matches`
- `should cap size at 100`
- `should default page to 0 and size to 20`

#### NotificationSentEventHandlerTest
- `should update sentAt and status=SENT`
- `should be idempotent — re-processing same event yields same state`
- `should not overwrite existing failureReason`

#### NotificationFailedEventHandlerTest
- `should update failureReason and status=FAILED`
- `should be idempotent`

#### NotificationViewResponseTest
- `from should map all NotificationView fields including lifecycle`
- `from should map null optional fields correctly`

#### PaginatedNotificationResponseTest
- `should calculate totalPages correctly`
- `should handle zero totalElements`

### 3.2 Integration Tests

#### NotificationControllerListIT (`@QuarkusTest`)
- `GET /notifications returns 200 with paginated response`
- `GET /notifications?page=0&size=5 returns correct page`
- `GET /notifications?type=EMAIL filters by type`
- `GET /notifications?status=SENT filters by status`
- `GET /notifications?createdFrom=...&createdTo=... filters by date range`
- `GET /notifications?type=EMAIL&status=SENT combines filters`
- `GET /notifications?page=999 returns empty content for beyond-last page`
- `GET /notifications?size=0 returns 400`
- `GET /notifications?size=101 returns 400 or caps at 100`

#### NotificationControllerGetIT (`@QuarkusTest`)
- `GET /notifications/{id} returns full lifecycle fields`
- `GET /notifications/{id} returns status=PENDING for newly created notification`
- `GET /notifications/{id} returns status=SENT after sent event projection`
- `GET /notifications/{id} returns status=FAILED after failed event projection`
- `GET /notifications/{unknown-id} returns 404`

#### NotificationSentEventHandlerIT
- `consume sent event from Kafka → NotificationView.sentAt set, status=SENT`
- `re-consume same event → no change (idempotent)`

#### NotificationFailedEventHandlerIT
- `consume failed event from Kafka → NotificationView.failureReason set, status=FAILED`
- `re-consume same event → no change (idempotent)`

#### MongoNotificationViewRepositoryIT
- `findAll with type filter returns correct documents`
- `findAll with status filter returns correct documents`
- `findAll with date range filter returns correct documents`
- `countAll returns correct total`
- `compound index (type, status, createdAt) exists on notifications collection`

#### MongoIndexInitializerIT
- `should create compound index on startup`
- `should not fail if index already exists`

### 3.3 Regression Tests
- `existing GET /notifications/{id} still works for notifications without status field (backward compat)`
- `existing NotificationCreatedEventHandler sets status=PENDING`

## 4. Quality Gates

### Entry Criteria
- Delivery Pipeline (F3) projectors and events defined
- `NotificationView` updated with new fields
- MongoDB dev services available for integration tests

### Exit Criteria
- All unit and integration tests pass
- ≥ 80 % line coverage
- List query benchmark < 100 ms p99
- Compound index verified in integration test
- Code review approved
- OpenAPI spec includes list endpoint with filter params

## 5. Test Estimation

| Test Category | Estimated Effort |
|--------------|-----------------|
| Unit tests (handlers, DTOs, query handler) | 1.5 SP |
| Integration tests (REST list + get) | 2 SP |
| Integration tests (projectors, MongoDB) | 1.5 SP |
| Regression tests | 0.5 SP |
| **Total** | **5.5 SP** |
