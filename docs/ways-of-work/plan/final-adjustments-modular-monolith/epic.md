# Epic PRD: Final Adjustments for Modular Monolith

## 1. Epic Name
- **Name:** `final-adjustments-modular-monolith`

## 2. Goal
- **Problem:** While adopting a Modular Monolith architecture, the `notification` and `template` bounded contexts still exhibit direct domain-to-domain and application-to-domain coupling. For instance, `notification` directly imports `TemplateEngine` and `TemplateName` from the `template` context, and `template` imports `NotificationType` from the `notification` context. This direct coupling violates the isolation demanded by Domain-Driven Design and Hexagonal Architecture, meaning changes in one context can break the other.
- **Solution:** Execute the final architectural adjustments to achieve strict decoupling. This includes:
  1. Extracting shared cross-cutting domains (like `NotificationType`) into the Shared Kernel so both contexts can use them independently.
  2. Defining explicit application-level contracts (DTOs or simple interfaces) for inter-module communication (e.g., resolving templates) to prevent domain leakage.
- **Impact:** Ensures the codebase operates as a true Modular Monolith. Modifying or completely replacing the internals of `template` will have zero breaking impact on `notification` (and vice versa) as long as the application contracts are respected.

## 3. User Personas
- **Software Engineers & Architects:** The primary beneficiaries. They need a pristine architecture where boundaries are 100% respected, making development, testing, and potential future microservices extraction trivial.

## 4. High-Level User Journeys
- **As an Architect**, I want to perform a static analysis of the codebase and see zero forbidden cross-context imports between `notification` and `template`.
- **As a Developer**, I want to implement a change in the `template`'s internal rendering engine without having to modify or even recompile the `notification` context.
- **As a Developer**, I want to easily add a new `NotificationType` to the Shared Kernel so that both contexts can inherently support it without circular dependencies.

## 5. Business Requirements

### Functional Requirements
- **Extract NotificationType:** Move `NotificationType` (and any other universally shared domain enums/concepts) to the `shared/domain` package.
- **Abstract Template Engine Call:** Refactor `CreateNotificationHandler` in the `notification` context so it no longer directly references `TemplateEngine` or `TemplateName`. Instead, it should rely on an application-level Port/Contract exposed explicitly for cross-module communication.
- **Remove Cross-Imports:** Eliminate all direct imports from `br.com.olympus.hermes.template.*` inside `br.com.olympus.hermes.notification.*` and vice versa.

### Non-Functional Requirements
- **Architectural Strictness:** Ensure 100% compliance with Hexagonal Architecture and Modular Monolith principles described in the project rules (`general.md`).
- **Functional Error Handling:** The newly introduced contracts must continue to return `Either<BaseError, T>` seamlessly integrating with Arrow-kt.
- **Test Integrity:** All existing unit and integration tests must pass. Any tests mocking the `TemplateEngine` in the `notification` context will need to be updated to mock the new Contract/Port.

## 6. Success Metrics
- **Zero Cross-Context Domain Imports:** Tools like `grep` reveal no domain or application imports crossing the boundary between `notification` and `template`.
- **Build Success:** `./mvnw clean verify` completes successfully.
- **Code Scents:** Architectural purity is restored, evaluated by a successful code review.

## 7. Out of Scope
- Adding new functional features (e.g., new types of notifications, new template syntaxes).
- Changing infrastructure components (Kafka, databases, etc.).

## 8. Business Value
- **High:** Paying off this critical technical debt unlocks the true value of the Modular Monolith, ensuring that development pace does not slow down as the complexity of individual bounded contexts grows.
