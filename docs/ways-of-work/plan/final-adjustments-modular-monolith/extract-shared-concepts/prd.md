# Feature PRD: Extract Shared Domain Concepts

## 1. Feature Name
- **Name:** Extract Shared Domain Concepts (`extract-shared-concepts`)

## 2. Epic
- **Epic:** [Final Adjustments for Modular Monolith](../epic.md)
- **Architecture:** [Architecture Specification](../arch.md)

## 3. Goal
- **Problem:** Building blocks like `NotificationType` are currently defined inside the `notification` domain, yet they are needed by the `template` domain to successfully resolve notification text. This cross-boundary dependency breaks the isolation principle of a modular monolith.
- **Solution:** Extract these common blocks and schemas (e.g., `NotificationType`) from the `notification` context and promote them to the `shared/domain` kernel boundaries.
- **Impact:** Alleviates circular dependency risks and creates a pristine boundary where both `template` and `notification` rely safely on the shared kernel without knowing about each other’s internal domains.

## 4. User Personas
- **Software Engineers & Architects:** The developers building and maintaining Hermeskt.

## 5. User Stories
- **As a Developer,** I want shared concepts like notification channels to live in the Shared Kernel so that I can use them in the Template context without importing anything from the Notification context.
- **As an Architect,** I want the Shared Kernel to contain all universally shared domain models so that I can enforce strict boundary linting rules across all contexts.

## 6. Requirements
### Functional Requirements
- Move `br.com.olympus.hermes.notification.domain.factories.NotificationType` to `br.com.olympus.hermes.shared.domain.core.NotificationChannelType` (or keep the name `NotificationType` but in the `shared/domain` package).
- Refactor all usages of this enum across both `notification` and `template` contexts to point to the new package.
- Ensure any related value objects or small enums that are intrinsically required by multiple contexts are also moved, if applicable.

### Non-Functional Requirements
- **No functional changes:** The payload, JSON serialization, database persistence values, and external API behavior must remain identical.
- **Compilation:** The project must compile successfully (`./mvnw clean compile`).
- **Isolation:** The `shared/domain` package must *not* depend on any specific context (e.g., no imports to `notification` or `template`).

## 7. Acceptance Criteria
- [ ] The class `NotificationType` (or its equivalent) resides in `src/main/kotlin/br/com/olympus/hermes/shared/domain/`.
- [ ] `grep -r "br.com.olympus.hermes.notification.domain.factories.NotificationType"` returns 0 results in the `template` context.
- [ ] All unit and integration tests pass successfully.

## 8. Out of Scope
- Adding new notification types (e.g., Push, Slack).
- Changing the underlying string values stored in DBs or Kafka.
