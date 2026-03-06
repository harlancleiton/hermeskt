# Epic Product Requirements Document (PRD)

## 1. Epic Name
refactor-modular-monolith

## 2. Goal

**Problem:** 
The current Hermeskt codebase is organized by technical layers (e.g., `core/application`, `infrastructure/rest`, `shared/domain`) rather than by business domains. As the application grows, this "Package by Layer" approach leads to high coupling between different business concepts (like Notifications and Templates), making it difficult to maintain, test, and scale the system independently. It also hinders future potential migrations to microservices if bounded contexts grow large enough.

**Solution:** 
Refactor the architecture from a Layered Monolith to a Modular Monolith built on Domain-Driven Design (DDD) principles. This involves reorganizing the package structure so that top-level modules represent Bounded Contexts (`notification`, `template`, and a `shared` kernel). The Hexagonal Architecture layers (domain, application, infrastructure) will be encapsulated *within* these cohesive business modules.

**Impact:** 
- Improved developer experience through better code discoverability and isolation.
- Enhanced maintainability with clear boundaries reducing unintended side-effects.
- Foundation laid for scalability and easier extraction of microservices if necessary.
- Tighter alignment between business domains and the codebase.

## 3. User Personas
- **Internal Development Team (Software Engineers & Architects):** Primary beneficiaries who will experience less friction when adding new features or modifying existing domains.
- **QA Engineers:** Benefit from more isolated domains allowing for more targeted integration and unit testing.

## 4. High-Level User Journeys
*N/A - This is a technical refactoring epic, so the primary journey is the developer workflow.*
- A developer locates all code related to "Templates" in a single cohesive `template` package, minimizing navigation across the entire project structure.
- A developer modifies the `notification` domain without worrying about unintended breakages in the `template` domain, thanks to enforced module boundaries.
- New team members easily understand the business subdomains just by looking at the top-level directory structure.

## 5. Business Requirements

**Functional Requirements:**
- Refactor the codebase structure such that the top-level packages reflect the business domains (`notification`, `template`, `shared`), moving away from technical layers.
- The application must continue to function exactly as before, with no changes to external APIs, input/output formats, or behavior.
- Existing REST endpoints, Kafka consumers/producers, and database interactions must remain fully operational.

**Non-Functional Requirements:**
- **Architecture Integrity:** Top-level packages must strictly define Bounded Contexts.
- **Encapsulation:** Technical layers (domain, application, infrastructure) must reside within their respective Bounded Context package.
- **Dependency Rules:** 
  - The `notification` context may depend on the `template` context via an explicit interface or port (Supplier-Customer pattern), but not directly on its infrastructure or internal logic.
  - The `shared` kernel must only contain generic building blocks (e.g., `BaseEntity`, `DomainEvent`, `BaseError`, CQRS interfaces) and must not depend on any specific bounded context.
- **Quality Gates:** The project must successfully compile (`./mvnw compile`) and all unit and integration tests must pass without functional changes.
- **Formatting:** Code must adhere to existing formatting standards (`./mvnw ktlint:format`).

## 6. Success Metrics
- 100% of existing tests continue to pass after the refactoring.
- 0 regressions reported in manual or automated QA cycles.
- Successful relocation of 100% of the domain, application, and infrastructure classes into their correct modular packages.

## 7. Out of Scope
- Adding new business features or changing existing business logic.
- Splitting the application into physical microservices (separate deployable units).
- Changing the underlying technology stack (Kotlin, Quarkus, DynamoDB, MongoDB, Kafka).
- Modifying the underlying data schema or migration scripts, unless refactoring necessitates it without altering the schema itself.

## 8. Business Value
**High**. While it doesn't deliver direct customer-facing features, reorganizing into a Modular Monolith drastically reduces technical debt, improves engineering velocity for all future features, and sets up a scalable architectural foundation that protects the application from becoming a "Big Ball of Mud".
