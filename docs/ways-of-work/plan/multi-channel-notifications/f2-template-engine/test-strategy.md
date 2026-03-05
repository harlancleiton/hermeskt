# Test Strategy: Template Engine

## 1. Test Strategy Overview

### Testing Scope

- `TemplateName` and `TemplateBody` value objects
- `NotificationTemplate` domain entity
- `TemplateEngine` domain service (resolve + interpolate)
- `TemplateRepository` port and `MongoTemplateRepository` implementation
- `CachingTemplateRepository` decorator (Caffeine cache)
- CQRS handlers: `CreateTemplateHandler`, `UpdateTemplateHandler`, `DeleteTemplateHandler`, `GetTemplateQueryHandler`, `ListTemplatesQueryHandler`
- `TemplateController` REST endpoints (CRUD)
- Integration of template resolution into `CreateNotificationHandler`
- New error types: `TemplateNotFoundError`, `MissingTemplateVariablesError`, `TemplateDuplicateError`, `InvalidTemplateNameError`, `InvalidTemplateBodyError`

### Quality Objectives

- ≥ 80 % line coverage for all new code
- 100 % acceptance criteria validation
- Template resolution (cache hit) < 5 ms p99
- Zero regressions on existing notification creation flow (raw content)

### Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Regex ReDoS on malformed template body | Low | High | Limit body size to 64 KB; use possessive quantifiers; fuzz test |
| Cache stale after update/delete | Medium | High | Explicit invalidation on write + TTL fallback; integration test |
| Backward-compatibility break (templateName=null) | Low | High | Explicit regression test: send notification without templateName |
| Placeholder collision with literal `{{` in content | Low | Medium | Document escape convention; boundary test with `\{\{` literals |

## 2. Test Design Techniques (ISTQB)

### Equivalence Partitioning

| Input | Valid Partition | Invalid Partitions |
|-------|---------------|-------------------|
| `templateName` (slug) | `^[a-z0-9]+(-[a-z0-9]+)*$`, 1–128 chars | Blank, uppercase, special chars, > 128 chars |
| `templateBody` | Non-blank, ≤ 64 KB | Blank, > 64 KB |
| `payload` vs placeholders | All placeholders covered | Missing one or more keys |
| `channel` query param | EMAIL, SMS, PUSH, WHATSAPP | Unknown string, blank |

### Boundary Value Analysis

| Boundary | Test Values |
|----------|------------|
| `templateName` length | 1 char (`a`), 128 chars, 129 chars |
| `templateBody` size | 1 char, 65536 chars, 65537 chars |
| Placeholder count | 0, 1, 50 (stress) |
| Payload with extra keys | All required + extras → success (extras ignored) |

### Decision Table: Template Resolution

| templateName provided | Template exists | All variables present | Expected |
|:---:|:---:|:---:|:---|
| N | — | — | Use raw `content` (backward-compat) |
| Y | N | — | `TemplateNotFoundError` (404) |
| Y | Y | N | `MissingTemplateVariablesError` (400) |
| Y | Y | Y | Interpolated content → aggregate created |

## 3. Test Plan

### 3.1 Unit Tests

#### TemplateNameTest
- `should create valid slug`
- `should reject blank value`
- `should reject uppercase characters`
- `should reject special characters`
- `should reject value exceeding 128 chars`
- `should accept single character`
- `should accept hyphenated slug`

#### TemplateBodyTest
- `should create valid body`
- `should reject blank body`
- `should reject body exceeding 64 KB`
- `should accept body at exactly 64 KB`

#### TemplateEngineTest
- `resolve should interpolate all placeholders`
- `resolve should interpolate subject for EMAIL channel`
- `resolve should return TemplateNotFoundError when template missing`
- `resolve should return MissingTemplateVariablesError when payload incomplete`
- `resolve should list ALL missing variables, not just first`
- `resolve should succeed when payload has extra keys beyond placeholders`
- `resolve should return body unchanged when no placeholders present`
- `resolve should handle nested dot-notation variable names`
- `resolve should not fail on literal double-braces that are not valid placeholders`

#### CreateTemplateHandlerTest
- `should create template successfully`
- `should return TemplateDuplicateError when name+channel exists`
- `should validate TemplateName and TemplateBody VOs`

#### UpdateTemplateHandlerTest
- `should update template successfully`
- `should return TemplateNotFoundError when template does not exist`

#### DeleteTemplateHandlerTest
- `should delete template successfully`
- `should return TemplateNotFoundError when template does not exist`

#### GetTemplateQueryHandlerTest
- `should return template when found`
- `should return null when not found`

#### ListTemplatesQueryHandlerTest
- `should return paginated list`
- `should filter by channel`
- `should return empty list when no templates match`

#### CreateNotificationHandler (Template Integration)
- `should resolve template and use interpolated content`
- `should use raw content when templateName is null`
- `should propagate TemplateNotFoundError`
- `should propagate MissingTemplateVariablesError`

### 3.2 Integration Tests

#### TemplateControllerIT (`@QuarkusTest`)
- `POST /templates returns 201 for valid request`
- `POST /templates returns 409 for duplicate name+channel`
- `POST /templates returns 400 for invalid name or body`
- `GET /templates/{name}?channel=EMAIL returns 200`
- `GET /templates/{name}?channel=EMAIL returns 404 when not found`
- `GET /templates returns paginated list`
- `GET /templates?channel=SMS filters correctly`
- `PUT /templates/{name}?channel=EMAIL returns 200`
- `PUT /templates/{name}?channel=EMAIL returns 404 when not found`
- `DELETE /templates/{name}?channel=EMAIL returns 204`
- `DELETE /templates/{name}?channel=EMAIL returns 404 when not found`

#### CachingTemplateRepositoryIT
- `should return cached template on second call (no MongoDB hit)`
- `should invalidate cache on update`
- `should invalidate cache on delete`
- `should populate cache on miss`

#### CreateNotificationWithTemplateIT
- `POST /notifications/email with templateName resolves template into content and subject`
- `POST /notifications/email without templateName uses raw content (backward-compat)`
- `POST /notifications/email with non-existent templateName returns 404`
- `POST /notifications/email with missing payload variables returns 400`

### 3.3 Regression Tests
- `existing Email notification creation without template still works`
- `existing SMS notification creation without template still works`
- `existing WhatsApp notification creation without template still works`

## 4. Quality Gates

### Entry Criteria
- All implementation tasks completed
- Code compiles (`./mvnw compile`)
- No new Kotlin warnings

### Exit Criteria
- All unit and integration tests pass
- ≥ 80 % line coverage on new code
- Template resolution benchmark < 5 ms (cache hit)
- No critical/high severity defects
- Code review approved
- OpenAPI spec includes `/templates` endpoints

## 5. Test Estimation

| Test Category | Estimated Effort |
|--------------|-----------------|
| Unit tests (VOs, TemplateEngine, handlers) | 2 SP |
| Integration tests (REST CRUD) | 2 SP |
| Integration tests (cache, notification+template) | 1.5 SP |
| Regression tests | 0.5 SP |
| **Total** | **6 SP** |
