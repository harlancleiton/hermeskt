# Feature PRD: Notification Context Migration

## 1. Feature Name
Notification Context Migration

## 2. Epic
- **Epic PRD**: `/docs/ways-of-work/plan/refactor-modular-monolith/epic.md`
- **Architecture**: `/docs/ways-of-work/plan/refactor-modular-monolith/arch.md`

## 3. Goal
**Problem:** The core Notification business logic is currently spread across technical layers (`shared/domain`, `core/application`, `infrastructure`). After extracting the Shared Kernel and the Template Context, the remaining code forms the core Notification Bounded Context, but it is still sitting in the legacy layer-based directories.
**Solution:** Relocate all remaining notification-specific code into a dedicated, cohesive Bounded Context package named `br.com.olympus.hermes.notification`. This package will contain its own Hexagonal Architecture layers (`domain`, `application`, `infrastructure`).
**Impact:** Completes the Modular Monolith transition. The Notification domain becomes fully encapsulated, making it trivial to understand the boundaries of the system's primary capability. It ensures future additions to the Notification domain are isolated from the rest of the system.

## 4. User Personas
- **Internal Development Team (Software Engineers & Architects)**

## 5. User Stories
- As a Software Engineer, I want all notification-related code (entities, handlers, adapters) to be isolated in a `notification` package so that the core domain is cohesive and decoupled from the broader application structure.

## 6. Requirements
**Functional Requirements:**
- The `notification` package must be created at the root level (`br.com.olympus.hermes.notification`).
- All Notification domain models (`Notification`, `EmailNotification`, `SmsNotification`, `WhatsAppNotification`, `PushNotification`, plus related value objects and factories) must be moved to `notification/domain`.
- All Notification use cases (e.g., `CreateNotificationHandler`, `ListNotificationsQueryHandler`), projectors, and port interfaces (`NotificationProviderAdapter`) must be moved to `notification/application`.
- All Notification infrastructure (e.g., `NotificationController`, Kafka Consumers like `NotificationCreatedConsumer`, DynamoDB records, and external provider integrations like Twilio/SES adapters) must be moved to `notification/infrastructure`.

**Non-Functional Requirements:**
- **Boundary Enforcement:** The `notification` context is permitted to depend on the `template` context (Supplier-Customer) and the `shared` context.
- **Cleanup:** Once all code is moved into `notification`, the original legacy packages (`core`, and the non-shared parts of `infrastructure` and `shared`) must be empty and safely deleted.

## 7. Acceptance Criteria
- [ ] The `br.com.olympus.hermes.notification` package contains all and only notification-related classes, organized by `domain`, `application`, and `infrastructure` layers.
- [ ] Legacy root packages (`core`) and non-shared sub-packages are deleted.
- [ ] Compilation succeeds (`./mvnw compile`).
- [ ] All Notification-related unit and integration tests are moved to the corresponding structure in `src/test/kotlin` and pass successfully.

## 8. Out of Scope
- Modifying the Kafka topic names or DynamoDB table configurations.
- Changing how provider integrations actually deliver messages.
