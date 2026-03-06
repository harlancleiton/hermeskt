# Feature PRD: Decouple Notification Template Handler

## 1. Feature Name
- **Name:** Decouple Notification Template Handler (`decouple-template-handler`)

## 2. Epic
- **Epic:** [Final Adjustments for Modular Monolith](../epic.md)
- **Architecture:** [Architecture Specification](../arch.md)

## 3. Goal
- **Problem:** The `notification` bounded context injects `TemplateEngine` directly. This directly couples the `notification` application logic to the `template` domain logic. If `TemplateEngine`'s signature or internal logic changes, it forces a change in `notification`, which contradicts the Modular Monolith principles.
- **Solution:** Introduce an explicit application port (contract/interface) in the `notification` context (e.g., `TemplateResolverPort`) and implement it via an Adapter that communicates with a well-defined Application Service in the `template` context.
- **Impact:** Achieves complete encapsulation and loose coupling between contexts. The `template` context becomes a black-box service provider to the `notification` context.

## 4. User Personas
- **Software Engineers & Architects:** The developers building and maintaining Hermeskt.

## 5. User Stories
- **As a Developer,** I want the `notification` context to resolve templates using a generic, boundary-respecting interface so that I don't have to import the internal classes of the `template` domain.
- **As an Architect,** I want the boundary between `notification` and `template` strictly governed by DTOs or primitive values, guaranteeing isolated testability and future service extraction.

## 6. Requirements
### Functional Requirements
- **Create Outbound Port:** Create an interface in `notification/application/ports` (e.g., `TemplateResolverPort`) that exposes a function to resolve a template (taking generic inputs like name, channel, payload).
- **Update Handler:** Refactor `CreateNotificationHandler` to depend exclusively on `TemplateResolverPort` instead of `TemplateEngine`.
- **Implement Adapter:** Provide an infrastructure or cross-boundary dependency injection adapter that implements `TemplateResolverPort` by delegating to the `template` context's application layer (e.g., `ResolveTemplateQueryHandler` or a dedicated facade).

### Non-Functional Requirements
- **No functional changes:** Templates must resolve exactly as before, with the same payload interpolation.
- **Compilation:** The project must compile successfully (`./mvnw clean compile`).
- **Isolation:** `CreateNotificationHandler` and the `notification/domain` must contain zero imports containing `br.com.olympus.hermes.template.*`.
- **Error Handling:** The new port must continue to return `Either<BaseError, ResolvedTemplateDto>` using Arrow-kt.

## 7. Acceptance Criteria
- [ ] The `CreateNotificationHandler` class only imports classes from `notification`, `shared`, or standard libraries.
- [ ] A new port interface is defined for resolving templates.
- [ ] An adapter cleanly bridges the gap between the contexts using Dependency Injection or a simple Facade.
- [ ] All unit and integration tests (specifically those testing notification creation) pass successfully. Mocking in tests should target the Port, not the `TemplateEngine`.

## 8. Out of Scope
- Changing the syntax or rendering logic inside the `template` context.
- Adding asynchronous template resolution via Kafka (this integration remains synchronous locally).
