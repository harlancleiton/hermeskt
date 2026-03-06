# Test Strategy: Decouple Notification Template Handler

## Test Strategy Overview
This strategy outlines the testing approach for refactoring `CreateNotificationHandler` to use the new `TemplateResolver` port instead of the direct `TemplateEngine`. While functionally the system remains identical, the dependency injection and structural boundaries change significantly. The primary goal is to ensure that the logic of template resolution inside notification creation remains perfectly intact while verifying that the new Adapter properly bridges the gap to the `template` context.

## ISTQB Framework Application

**Test Design Techniques Used:**
- [x] Equivalence Partitioning (applied to existing tests regarding resolving vs. not resolving a template based on `type`).
- [ ] Boundary Value Analysis
- [ ] Decision Table Testing
- [ ] State Transition Testing
- [x] Experience-Based Testing (verifying Quarkus CDI injection behavior for the new Adapter).

**Test Types Coverage:**
- [x] Functional Testing (Ensuring the Handler still produces a `ResolvedTemplateDto` correctly).
- [ ] Non-Functional Testing 
- [x] Structural Testing (Static analysis of imports and validating Port-Adapter pattern).
- [x] Change-Related Testing (Heavy Regression on `CreateNotificationHandlerTest`).

## ISO 25010 Quality Characteristics

**Priority Assessment:**
- [x] Functional Suitability: **Critical** (The handler must compile the email subject and body correctly as before).
- [ ] Performance Efficiency: Low
- [ ] Compatibility: Low
- [ ] Usability: Low
- [x] Reliability: **High** (Ensuring DTO mapping between the Adapter and `TemplateEngine` doesn't drop fields or throw unexpected exceptions).
- [ ] Security: Low
- [x] Maintainability: **Critical** (This refactoring decouples the architecture, increasing long-term maintainability).
- [ ] Portability: Low

## Quality Gates

**Entry Criteria:**
- The `TemplateResolver` port is defined in `notification/application/ports`.
- The `TemplateResolverAdapter` is implemented in `notification/infrastructure`.

**Exit Criteria:**
- `./mvnw clean compile` succeeds.
- `./mvnw test` reports 100% pass rate.
- `CreateNotificationHandlerTest` is refactored: all `mockk<TemplateEngine>()` references are replaced with `mockk<TemplateResolver>()`.
- Zero imports containing `br.com.olympus.hermes.template.*` exist in `notification/application` and `notification/domain`.

## Test Environment and Data Strategy
Testing will be performed using existing JUnit 5 + MockK setups.
- **Unit Testing**: Refactor `CreateNotificationHandlerTest` to mock the new `TemplateResolver` interface.
- **Integration Testing**: An existing `@QuarkusTest` (if testing the full REST endpoint to DB creation flow) should naturally test the real `TemplateResolverAdapter` connecting to the real `TemplateEngine`. This will prove that CDI successfully wired the cross-context boundary.
