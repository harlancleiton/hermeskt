# Issues Checklist: mount-notifications

## Pre-Creation Preparation
- [x] Feature artifacts complete (PRDs, Implementation Plans, Test Strategies).
- [ ] Epic exists in GitHub (Run issue creation script).
- [ ] Project board configured.
- [ ] Team capacity assessed.

---

## Epic Level Issue

- [ ] **[EPIC] mount-notifications**
  - **Labels**: `epic`, `priority-high`, `value-high`
  - **Milestone**: (TBD by PM)
  - **Estimate**: L
  - **Description**: Build an event-driven system to mount templates and dispatch notifications.

---

## Feature Level Issues

### F1: Event Listener Infrastructure
- [ ] **[FEATURE] f1-event-listener-infrastructure**
  - **Labels**: `feature`, `priority-high`, `infrastructure`
  - **Epic**: mount-notifications
  - **Estimate**: 5 pts
  - **Description**: Setup Kafka consumer and DLQ for initial event ingestion.

#### F1 Stories & Enablers
- [ ] **[STORY] Kafka Consumer Implementation**
  - **Labels**: `user-story`, `backend`, `priority-high`
  - **Estimate**: 3 pts
  - **Tasks**: Define `@Incoming` channel, Jackson deserialization wrapper, `@Blocking` behavior.
- [ ] **[ENABLER] DLQ Infrastructure Setup**
  - **Labels**: `enabler`, `infrastructure`, `priority-high`
  - **Estimate**: 1 pt
  - **Tasks**: Configure `application.properties` for SmallRye DLQ routing.
- [ ] **[TEST] F1 Integration Tests**
  - **Labels**: `test`, `integration-test`
  - **Estimate**: 1 pt
  - **Tasks**: Testcontainers Redpanda setup, assert JSON payload deserialization and explicit DLQ test.

---

### F2: Template Mounting Engine
- [ ] **[FEATURE] f2-template-mounting-engine**
  - **Labels**: `feature`, `priority-high`, `backend`
  - **Epic**: mount-notifications
  - **Estimate**: 8 pts
  - **Description**: Domain service to resolve templates and safely interpolate JSON payloads.
  - **Dependencies**: Blocks F3.

#### F2 Stories & Enablers
- [ ] **[STORY] Template Engine Integration (Pebble/Mustache)**
  - **Labels**: `user-story`, `backend`, `priority-high`
  - **Estimate**: 3 pts
  - **Tasks**: Implement `TemplateMountingService` using selected library, handle missing variables securely.
- [ ] **[ENABLER] Event-to-Template Resolver Port**
  - **Labels**: `enabler`, `database`, `priority-medium`
  - **Estimate**: 3 pts
  - **Tasks**: Define SPI Port, implement MongoDB adapter to read from `TemplateView`.
- [ ] **[TEST] F2 Unit & Interpolation Tests**
  - **Labels**: `test`, `unit-test`, `security-test`
  - **Estimate**: 2 pts
  - **Tasks**: Test deep-nested JSON resolving, XSS prevention checks, timing performance logic.

---

### F3: Notification Dispatch Flow
- [ ] **[FEATURE] f3-notification-dispatch-flow**
  - **Labels**: `feature`, `priority-high`, `backend`
  - **Epic**: mount-notifications
  - **Estimate**: 13 pts
  - **Description**: CQRS command flow to record intent to send, save to DynamoDB, dispatch via output port, and project to MongoDB.
  - **Dependencies**: Blocked by F1, F2 (for full E2E, but can start internally).

#### F3 Stories & Enablers
- [ ] **[STORY] CQRS Command Flow & Notification Aggregate**
  - **Labels**: `user-story`, `backend`, `priority-high`
  - **Estimate**: 3 pts
  - **Tasks**: Implement `CreateNotificationCommand`, `CreateNotificationHandler`, `Notification` aggregate root, DynamoDB repository save.
- [ ] **[ENABLER] Provider Port & Dummy Adapter**
  - **Labels**: `enabler`, `api`, `priority-high`
  - **Estimate**: 2 pts
  - **Tasks**: Define `NotificationProviderPort`, create logging adapter, configure Resilience4j retries.
- [ ] **[STORY] Read Model Projector**
  - **Labels**: `user-story`, `database`, `priority-high`
  - **Estimate**: 3 pts
  - **Tasks**: Implement `NotificationCreatedProjector`, Kafka `@Incoming` consumer, `NotificationView` Panache entity.
- [ ] **[TEST] F3 E2E Integration Tests**
  - **Labels**: `test`, `e2e-test`, `quality-gate`
  - **Estimate**: 5 pts
  - **Tasks**: Complete end-to-end QuarkusTest asserting DynamoDB write -> Provider call -> Kafka emit -> MongoDB View creation.
