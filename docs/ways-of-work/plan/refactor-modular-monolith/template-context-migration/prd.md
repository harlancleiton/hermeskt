# Feature PRD: Template Context Migration

## 1. Feature Name
Template Context Migration

## 2. Epic
- **Epic PRD**: `/docs/ways-of-work/plan/refactor-modular-monolith/epic.md`
- **Architecture**: `/docs/ways-of-work/plan/refactor-modular-monolith/arch.md`

## 3. Goal
**Problem:** Template-related classes (Entities, Value Objects, Use Cases, Controllers, and Repositories) are currently scattered across technical layers (`shared/domain`, `core/application`, `infrastructure`). This mixes Template logic with Notification logic, making it harder to discern the boundaries of the Template Management capability.
**Solution:** Relocate all template-specific code into a dedicated, cohesive Bounded Context package named `br.com.olympus.hermes.template`. This package will internally contain its own Hexagonal Architecture layers (`domain`, `application`, `infrastructure`).
**Impact:** Drastically improves code discoverability and cohesion for the Template Management subdomain. It enforces the Modular Monolith structure and prevents accidental tight coupling with the Notification context.

## 4. User Personas
- **Internal Development Team (Software Engineers & Architects)**

## 5. User Stories
- As a Software Engineer, I want all template-related code to be isolated in a `template` package so that I can easily find and modify template rendering logic without navigating through the entire project.

## 6. Requirements
**Functional Requirements:**
- The `template` package must be created at the root level (`br.com.olympus.hermes.template`).
- All Template domain models (`NotificationTemplate`, `ResolvedTemplate`, `TemplateBody`, `TemplateName`) and port interfaces (`TemplateEngine`, `TemplateRepository`) must be moved to `template/domain`.
- All Template use cases (e.g., `CreateTemplateHandler`, `UpdateTemplateHandler`) and their corresponding Commands/Queries must be moved to `template/application`.
- All Template infrastructure (e.g., `TemplateController`, MongoDB Panache entities/repositories) must be moved to `template/infrastructure`.

**Non-Functional Requirements:**
- **Boundary Enforcement:** The `template` context MUST NOT depend on any internal implementations of the `notification` context. It acts purely as a Supplier context.
- **Shared Kernel:** The `template` context is permitted to depend on the `shared` context extracted in the previous feature.

## 7. Acceptance Criteria
- [ ] The `br.com.olympus.hermes.template` package contains all and only template-related classes, neatly organized by `domain`, `application`, and `infrastructure` layers.
- [ ] Compilation succeeds (`./mvnw compile`).
- [ ] All Template-related unit and integration tests are moved to the corresponding structure in `src/test/kotlin` and pass successfully.
- [ ] Access modifiers (if applicable) are tightened to package-private where classes do not need to be exposed outside the `template` context.

## 8. Out of Scope
- Rewriting the template rendering engine logic.
- Altering the MongoDB document schema.
