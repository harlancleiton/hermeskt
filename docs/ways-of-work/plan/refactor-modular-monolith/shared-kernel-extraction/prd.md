# Feature PRD: Shared Kernel Extraction

## 1. Feature Name
Shared Kernel Extraction

## 2. Epic
- **Epic PRD**: `/docs/ways-of-work/plan/refactor-modular-monolith/epic.md`
- **Architecture**: `/docs/ways-of-work/plan/refactor-modular-monolith/arch.md`

## 3. Goal
**Problem:** Core architectural building blocks like `BaseEntity`, `DomainEvent`, CQRS handler interfaces, and top-level exception handlers are currently mixed into `shared/domain` alongside concrete business entities like `Notification` and `NotificationTemplate`. This muddies the distinction between domain-agnostic foundation code and business-specific code.
**Solution:** Extract these foundational, domain-agnostic building blocks into a strictly defined `shared` module that acts as the Shared Kernel. This kernel will be the only module permitted to be globally depended upon by other Bounded Contexts.
**Impact:** Clears out technical noise from business domains, establishes a firm architectural foundation that all Bounded Contexts can rely on without causing circular dependencies or business-logic bleeding.

## 4. User Personas
- **Internal Development Team (Software Engineers & Architects)**

## 5. User Stories
- As a Software Engineer, I want the `shared` package to only contain generic architectural abstractions (AggregateRoot, CommandHandler, DomainExceptionMapper) so that I clearly know what code is safe to import across the entire application.

## 6. Requirements
**Functional Requirements:**
- The `shared` package must contain the `DomainEvent` interface, `BaseEntity`, `AggregateRoot`.
- The `shared` package must contain CQRS marker interfaces (`Command`, `Query`, `CommandHandler`, `QueryHandler`, `EventHandler`).
- The `shared` package must contain the `BaseError` sealed hierarchy and `DomainExceptionMapper`.
- The `shared` package must contain the generic `PaginatedResult` wrapper.

**Non-Functional Requirements:**
- **Encapsulation:** Code inside the `shared` module must NOT reference, import, or depend on any code from the `notification` or `template` contexts.
- **Maintainability:** The extraction must preserve all existing HTTP response codes and entity persistence behaviors.

## 7. Acceptance Criteria
- [ ] `shared/domain` only contains generic interfaces/classes (no `Notification` or `Template` classes).
- [ ] `shared/application` only contains generic CQRS interfaces and ports.
- [ ] `shared/infrastructure` only contains generic mappers, configurations, and wrappers.
- [ ] Compilation succeeds (`./mvnw compile`).
- [ ] All tests pass without any modification to the test logic itself (only package imports changed).

## 8. Out of Scope
- Extracting the Shared Kernel into a separate Maven module or JAR file. (It remains a package within the Monolith).
