# Feature PRD: f2-template-mounting-engine

## 1. Feature Name
f2-template-mounting-engine

## 2. Epic
[mount-notifications](../epic.md)  
[Architecture](../arch.md)

## 3. Goal
**Problem:** Domain events contain raw data (like user IDs, raw 2FA codes, or specific event timestamps) that are unformatted. We need a way to combine this raw data with a pre-defined text template to create a human-readable message.
**Solution:** Build a Template Mounting Engine (a domain service) inside the `notification` bounded context. This engine will take an event payload and a `Template` aggregate (which could have variable placeholders like `{{user.name}}`), and securely resolve them to produce a finalized string payload.
**Impact:** This decoupling allows marketing or product teams to update template wording in the database without requiring code changes to the events or the mounting engine itself.

## 4. User Personas
- **System Service (Internal):** The engine acts automatically. Indirectly, it affects End Users who receive the beautifully formatted messages.

## 5. User Stories
- As a Hermes system component, I want to map a specific Kafka `eventType` to a specific `templateId` so I know which text structure to use for a given event.
- As a Hermes system component, I want to merge dynamic JSON payload variables into a text template securely so that the user receives personalized messages (e.g., "Hello, Jane").
- As a Hermes system component, I want the merge process to fail predictably (e.g., leaving a fallback or throwing a handled error) if a required template variable is missing from the payload payload so that I don't send broken messages.

## 6. Requirements

**Functional Requirements:**
- The engine must provide a method to accept: `EventType` (String), `TemplateBody` (String), and `Payload` (Map/JSON).
- The engine must identify placeholders within the `TemplateBody` (e.g., using a chosen syntax like Mustache/Handlebars or simple regex if sufficient).
- The engine must replace those placeholders with values derived from the `Payload`.
- The engine must handle nested data structures in the payload (e.g., resolving `user.profile.name` from `{"user": {"profile": {"name": "Alice"}}}`).
- The system must define an `EventToTemplateResolver` interface (port) to fetch the appropriate `Template` based on the `eventType`.

**Non-Functional Requirements:**
- **Performance:** Template evaluation should be blazing fast (< 10ms per template), heavily utilizing in-memory structures or fast parsing libraries over slow regex if the templates become complex.
- **Security:** The templating engine must be safe from code injection (e.g., if a user submits a malicious string as their name, the templating engine should only treat it as a literal string, not executable code).

## 7. Acceptance Criteria
- [ ] A template engine library (e.g., Apache FreeMarker, Pebble, or native Kotlin string templating logic) is integrated or implemented.
- [ ] The engine correctly evaluates simple top-level variables.
- [ ] The engine correctly evaluates nested variables.
- [ ] The engine has a defined fallback behavior for missing variables (e.g., throwing a `TemplateMountingError` extending `BaseError.ClientError`).
- [ ] Safe interpolation is verified via unit tests (no XSS or code execution via payload strings).

## 8. Out of Scope
- Creating the administrative UI to define templates (resides in the `template` context).
- Delivering the output string to AWS/Twilio (resides in `f3-notification-dispatch-flow`).
