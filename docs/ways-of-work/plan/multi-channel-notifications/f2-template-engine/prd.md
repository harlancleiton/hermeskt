# Feature PRD: Template Engine

## 1. Feature Name

**Dynamic Template Engine**

## 2. Epic

- [Epic PRD](../epic.md)
- [Architecture Spec](../arch.md)

## 3. Goal

### Problem

Consuming services must pre-render notification content before calling Hermes. This leads to duplicated rendering logic across multiple upstream services, inconsistent message formatting, and tight coupling between business logic and notification copy. Updating a notification message requires code changes and redeployment of the consuming service.

### Solution

Introduce a Template Engine within Hermes that allows callers to reference a named template and supply a variable map (`payload`). The engine resolves `{{variable}}` placeholders against the payload before the notification aggregate is created. Templates are stored in MongoDB and cached in-memory (Caffeine) for fast resolution. CRUD REST endpoints under `/templates` allow template management without code deployments.

### Impact

- Decouples message copy from consuming service code.
- Reduces time-to-market for notification copy changes from hours (redeploy) to seconds (API call).
- Ensures consistent formatting across all channels.
- Enables non-engineering stakeholders to manage templates via future admin UIs (API-first).

## 4. User Personas

- **Platform Developer**: Creates/updates templates via REST API; references templates when sending notifications.
- **Product Manager**: Defines notification copy and variables; manages templates via API (future: admin UI).

## 5. User Stories

- **US-1**: As a Platform Developer, I want to create a template via `POST /templates` with a name, channel, body (with `{{variable}}` placeholders), and optional subject, so that I can define reusable notification content.
- **US-2**: As a Platform Developer, I want to retrieve a template by name via `GET /templates/{name}` so that I can verify its content before referencing it.
- **US-3**: As a Platform Developer, I want to update a template via `PUT /templates/{name}` so that I can modify notification copy without redeploying consuming services.
- **US-4**: As a Platform Developer, I want to delete a template via `DELETE /templates/{name}` so that I can remove obsolete templates.
- **US-5**: As a Platform Developer, I want to list all templates via `GET /templates` with optional filtering by channel, so that I can browse available templates.
- **US-6**: As a Platform Developer, I want to send a notification with a `templateName` field so that the system resolves the template and interpolates my `payload` variables into the message body (and subject for Email).
- **US-7**: As a Platform Developer, I want a clear `404` error when referencing a non-existent template, so that I can diagnose integration issues quickly.
- **US-8**: As a Platform Developer, I want a clear `400` error listing missing variables when my `payload` does not supply all required placeholders, so that I can fix my request.

## 6. Requirements

### Functional Requirements

- **FR-1**: Templates MUST be stored in a dedicated MongoDB collection (`templates`) with fields: `name` (unique slug), `channel` (EMAIL, SMS, PUSH, WHATSAPP), `subject` (nullable — used only for EMAIL), `body` (template string with `{{variable}}` placeholders), `description` (optional), `createdAt`, `updatedAt`.
- **FR-2**: Template `name` MUST be unique per channel (composite uniqueness: `name` + `channel`).
- **FR-3**: The system MUST provide CRUD endpoints:
  - `POST /templates` — create a new template.
  - `GET /templates/{name}?channel={channel}` — retrieve a template by name and channel.
  - `GET /templates?channel={channel}` — list templates, optionally filtered by channel, with pagination.
  - `PUT /templates/{name}?channel={channel}` — update an existing template.
  - `DELETE /templates/{name}?channel={channel}` — soft-delete or hard-delete a template.
- **FR-4**: The Template Engine domain service MUST:
  - Accept a `templateName`, `channel`, and `payload` map.
  - Resolve the template from cache (hit) or MongoDB (miss).
  - Extract all `{{variable}}` placeholders from `body` (and `subject` for EMAIL).
  - Validate that the `payload` contains all required variables.
  - Return `Either<BaseError, ResolvedTemplate>` where `ResolvedTemplate` contains the interpolated `body` and optional `subject`.
- **FR-5**: If the template is not found, the engine MUST return a `TemplateNotFoundError` (maps to HTTP 404).
- **FR-6**: If required variables are missing, the engine MUST return a `MissingTemplateVariablesError` listing all missing variable names (maps to HTTP 400).
- **FR-7**: When `templateName` is provided in a notification creation request, the `CreateNotificationHandler` MUST invoke the Template Engine to resolve content before creating the aggregate.
- **FR-8**: When `templateName` is NOT provided, the handler MUST use the raw `content` field as-is (backward-compatible).
- **FR-9**: Templates MUST be cached in-memory (Caffeine) with a configurable TTL (default 5 min). Cache MUST be invalidated on template update/delete.
- **FR-10**: Template placeholder syntax is `{{variableName}}` — double curly braces with a valid identifier (letters, digits, underscores, dots).

### Non-Functional Requirements

- **NFR-1**: Template resolution (cache hit) MUST complete in < 5 ms p99.
- **NFR-2**: Template resolution (cache miss, MongoDB read) MUST complete in < 50 ms p99.
- **NFR-3**: All error paths MUST use Arrow-kt `Either<BaseError, T>`.
- **NFR-4**: The Template Engine MUST be a domain service with no infrastructure dependencies (MongoDB access via a port interface).
- **NFR-5**: All endpoints MUST be documented via SmallRye OpenAPI annotations.
- **NFR-6**: Template body MUST NOT exceed 64 KB.
- **NFR-7**: Template CRUD operations MUST be idempotent where applicable (PUT is full replace).

## 7. Acceptance Criteria

### US-1: Create Template

- **Given** a valid template payload; **When** `POST /templates` is called; **Then** the system returns `201 Created` with the template document.
- **Given** a template with the same `name` + `channel` already exists; **When** `POST /templates` is called; **Then** the system returns `409 Conflict`.

### US-2: Get Template

- **Given** a template exists with name `welcome-email` and channel `EMAIL`; **When** `GET /templates/welcome-email?channel=EMAIL` is called; **Then** the system returns `200` with the template.
- **Given** no template exists; **When** `GET /templates/unknown?channel=EMAIL` is called; **Then** the system returns `404`.

### US-3: Update Template

- **Given** an existing template; **When** `PUT /templates/{name}?channel={channel}` is called with updated body; **Then** the template is updated, cache is invalidated, and `200` is returned.

### US-4: Delete Template

- **Given** an existing template; **When** `DELETE /templates/{name}?channel={channel}` is called; **Then** the template is removed, cache is invalidated, and `204` is returned.

### US-5: List Templates

- **Given** templates exist; **When** `GET /templates?channel=EMAIL` is called; **Then** only EMAIL templates are returned with pagination metadata.

### US-6: Templated Notification

- **Given** a template `order-confirmation` exists with body `Hello {{userName}}, your order {{orderId}} is confirmed.`; **When** `POST /notifications/email` is called with `templateName=order-confirmation` and `payload={userName: "John", orderId: "123"}`; **Then** the notification is created with content `Hello John, your order 123 is confirmed.`.

### US-7: Template Not Found

- **Given** no template with name `nonexistent` exists; **When** a notification is sent referencing `templateName=nonexistent`; **Then** the system returns `404` with `TemplateNotFoundError`.

### US-8: Missing Variables

- **Given** a template with placeholders `{{a}}` and `{{b}}`; **When** a notification is sent with payload containing only `a`; **Then** the system returns `400` with `MissingTemplateVariablesError` listing `b`.

## 8. Out of Scope

- Admin UI / dashboard for template management.
- Template versioning / audit trail.
- Conditional logic within templates (if/else, loops).
- HTML rendering / MJML compilation for email templates.
- Template inheritance or partials.
- Internationalisation (i18n) / locale-based template selection.
