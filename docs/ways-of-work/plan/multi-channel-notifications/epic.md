# Epic: Multi-Channel Notification Delivery with Template Engine

## 1. Epic Name

**Multi-Channel Notification Delivery with Dynamic Template Engine**

## 2. Goal

### Problem

Today the Hermes notification service can **create** notification aggregates (Email, SMS, WhatsApp) and persist them via Event Sourcing, but it **does not actually deliver** them to end-users through external providers. There is also no Push Notification channel, and the existing `payload` field serves as an unstructured bag of key-value pairs with no formal template resolution mechanism. Teams that integrate with Hermes must pre-render notification content themselves, leading to duplicated rendering logic across consumers and inconsistent message formatting.

### Solution

Extend Hermes to:

1. **Deliver notifications** through channel-specific provider adapters (SMTP for Email, SMS gateway, WhatsApp Business API, and a new Push Notification provider such as Firebase Cloud Messaging).
2. **Add the Push Notification channel** — a new `PushNotification` aggregate, factory, events, and REST endpoint — following the same patterns already established for Email, SMS, and WhatsApp.
3. **Introduce a Template Engine** that allows callers to reference a named template and supply a variable map (`payload`). The engine resolves placeholders (e.g., `{{userName}}`) before the message is handed to the delivery adapter, enabling dynamic, reusable message content.

### Impact

- **Unified delivery**: all notification channels managed by a single service with consistent observability.
- **Reduced integration effort**: consuming services send a template name + variables instead of pre-rendered content.
- **Improved deliverability tracking**: sent/delivered/seen lifecycle events are published back through Kafka, enabling downstream analytics.
- **New channel coverage**: Push Notifications open mobile and web engagement scenarios.

## 3. User Personas

| Persona | Description |
|---|---|
| **Platform Developer** | Integrates with Hermes via REST API or Kafka to trigger notifications from upstream services (e.g., order service, auth service). Needs simple API contracts and clear error responses. |
| **Operations / SRE** | Monitors notification delivery health, DLQ depth, and provider latency. Needs structured logs, metrics, and idempotent retry semantics. |
| **Product Manager** | Defines notification templates and copy. Needs the ability to create/update templates without code deployments (future admin UI — out of scope for this epic, but the API must support it). |
| **End-User (Recipient)** | Receives notifications on their preferred channel. Expects timely, correctly personalised messages. |

## 4. High-Level User Journeys

### 4.1 Send a Templated Notification

1. Platform Developer calls `POST /notifications/{channel}` with a `templateName`, `payload` (variables), and recipient details.
2. Hermes validates the request, resolves the template by name, interpolates `payload` variables into the template body (and subject for Email), and creates the Notification aggregate.
3. The aggregate is persisted to DynamoDB (Event Store) and the `NotificationCreatedEvent` is published to Kafka.
4. A **Delivery Event Handler** (projector/saga) consumes the created event, invokes the appropriate channel provider adapter, and publishes a `NotificationSentEvent` (or `NotificationDeliveryFailedEvent`) back to Kafka.
5. Downstream projectors update the `NotificationView` in MongoDB with delivery status timestamps.

### 4.2 Send a Raw-Content Notification (No Template)

1. Same as above, but the caller provides `content` directly instead of `templateName`. The template resolution step is skipped.

### 4.3 Query Notification Status

1. Platform Developer calls `GET /notifications/{id}`.
2. Hermes returns the `NotificationView` from MongoDB, including delivery lifecycle timestamps (`sentAt`, `deliveryAt`, `seenAt`).

### 4.4 Manage Templates (CRUD)

1. Platform Developer calls template management endpoints (`POST /templates`, `GET /templates/{name}`, `PUT /templates/{name}`, `DELETE /templates/{name}`).
2. Templates are stored in MongoDB and cached in-memory for fast resolution during notification creation.

## 5. Business Requirements

### 5.1 Functional Requirements

#### Notification Channels

- **FR-1**: The system MUST support four notification channels: **Email**, **SMS**, **Push Notification**, and **WhatsApp**.
- **FR-2**: Each channel MUST have a dedicated REST endpoint (`POST /notifications/email`, `/sms`, `/push`, `/whatsapp`).
- **FR-3**: The Push Notification channel MUST be added as a new aggregate (`PushNotification`), with its own factory, domain events, command, request DTO, and provider adapter — following existing patterns for Email/SMS/WhatsApp.
- **FR-4**: Each channel MUST have a **provider adapter** (output port + infrastructure implementation) responsible for actual message delivery via external APIs.

#### Template Engine

- **FR-5**: The system MUST support named templates stored in MongoDB with a unique `name` (slug), `channel` (EMAIL, SMS, PUSH, WHATSAPP), `subject` (nullable, for Email), `body` (template string with `{{variable}}` placeholders), and metadata.
- **FR-6**: The system MUST provide CRUD REST endpoints for template management under `/templates`.
- **FR-7**: When a `templateName` is provided in the notification creation request, the system MUST resolve the template body (and subject for Email) and interpolate all `payload` variables before persisting the aggregate.
- **FR-8**: If a referenced template is not found, the system MUST return a `404` client error and NOT create the notification.
- **FR-9**: If the `payload` does not supply all required template variables, the system MUST return a `400` client error listing the missing variables.
- **FR-10**: When no `templateName` is provided, the system MUST use the raw `content` field as-is (backward-compatible).

#### Delivery Pipeline

- **FR-11**: After a `NotificationCreatedEvent` is projected, a **delivery handler** MUST asynchronously invoke the channel provider adapter.
- **FR-12**: On successful provider response, the system MUST publish a `NotificationSentEvent` containing the provider's shipping receipt.
- **FR-13**: On provider failure, the system MUST publish a `NotificationDeliveryFailedEvent` containing the error reason, and the message MUST be retried according to a configurable retry policy.
- **FR-14**: The `NotificationView` in MongoDB MUST be updated with `sentAt`, `deliveryAt`, `seenAt`, and `failureReason` as lifecycle events arrive.

#### Queries

- **FR-15**: `GET /notifications/{id}` MUST return full delivery status including lifecycle timestamps and failure reason if applicable.
- **FR-16**: `GET /notifications` MUST support pagination and filtering by `type`, `status` (pending, sent, delivered, failed), and date range.

### 5.2 Non-Functional Requirements

- **NFR-1**: Template resolution MUST complete in < 50 ms p99 (in-memory cache with MongoDB fallback).
- **NFR-2**: Delivery retries MUST use exponential backoff with a configurable max-retry count (default 3).
- **NFR-3**: All provider adapter calls MUST have configurable timeouts (default 10 s).
- **NFR-4**: The system MUST be idempotent: re-processing a `NotificationCreatedEvent` MUST NOT send duplicate messages to the provider (use aggregate version or idempotency key).
- **NFR-5**: Provider API keys and secrets MUST be injected via environment variables or Quarkus config — never hardcoded.
- **NFR-6**: All new endpoints MUST be documented via SmallRye OpenAPI annotations.
- **NFR-7**: The system MUST maintain the existing DDD / Hexagonal / CQRS / Event Sourcing architecture.
- **NFR-8**: All error paths MUST use Arrow-kt `Either<BaseError, T>` — no thrown exceptions in domain or application code.
- **NFR-9**: Test coverage: ≥ 80 % line coverage for new domain and application code; integration tests for every REST endpoint and every Kafka consumer.

## 6. Success Metrics

| KPI | Target |
|---|---|
| **Delivery Success Rate** | ≥ 99 % of accepted notifications delivered within 60 s |
| **Template Resolution Latency** | < 50 ms p99 |
| **API Availability** | ≥ 99.9 % uptime |
| **DLQ Depth** | < 100 messages at steady state |
| **Duplicate Delivery Rate** | 0 % (idempotent delivery) |
| **Mean Time to Detect Failure** | < 5 min (structured logs + metrics) |

## 7. Out of Scope

- **Admin UI / Dashboard** for template management (API-only for now).
- **Internationalisation (i18n)** of template content (future epic).
- **Recipient preference management** (e.g., "user X prefers WhatsApp over Email").
- **Batch / bulk notification sending** (single notification per request for now).
- **Rate limiting / throttling** at the Hermes API level (handled by API gateway).
- **Rich media attachments** (e.g., email attachments, WhatsApp media messages).
- **Two-way messaging / replies**.
- **Webhook callbacks** to consuming services on delivery status changes (consumers read Kafka events directly).

## 8. Business Value

**High** — This epic transforms Hermes from a notification *recording* service into a full notification *delivery* platform, unlocking:

- Centralised delivery logic eliminates duplicated provider integrations across consuming services.
- Template engine reduces time-to-market for new notification types from days to minutes.
- Push Notification channel opens mobile/web engagement, a critical gap today.
- Delivery tracking enables data-driven optimisation of communication strategies.
