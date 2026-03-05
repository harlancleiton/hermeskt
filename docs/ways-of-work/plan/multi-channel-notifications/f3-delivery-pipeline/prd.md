# Feature PRD: Notification Delivery Pipeline

## 1. Feature Name

**Asynchronous Notification Delivery Pipeline**

## 2. Epic

- [Epic PRD](../epic.md)
- [Architecture Spec](../arch.md)

## 3. Goal

### Problem

Hermes currently creates notification aggregates and persists them via Event Sourcing but never actually delivers messages to end-users. The `NotificationSentEvent` and `markAsSent()` method exist on the aggregate but are never invoked. There is no asynchronous process that picks up created notifications and routes them to channel-specific delivery providers.

### Solution

Introduce a Delivery Pipeline — an asynchronous Kafka consumer (`DeliveryHandler`) that listens to `NotificationCreatedEvent`s, reconstitutes the aggregate from the Event Store, invokes the appropriate channel provider adapter, and publishes lifecycle events (`NotificationSentEvent`, `NotificationDeliveryFailedEvent`) back through Kafka. The pipeline includes configurable retry with exponential backoff and idempotency guards to prevent duplicate deliveries.

### Impact

- Transforms Hermes from a notification recording service into a delivery platform.
- Provides a consistent, observable delivery path for all four channels.
- Enables delivery tracking and retry without consuming-service involvement.

## 4. User Personas

- **Platform Developer**: Expects notifications to be delivered after creation; monitors delivery status via query API.
- **Operations / SRE**: Monitors delivery health, retry rates, DLQ depth.

## 5. User Stories

- **US-1**: As a Platform Developer, I want notifications to be automatically delivered after creation so that I only need to call one API endpoint.
- **US-2**: As a Platform Developer, I want to see `sentAt` populated in the notification view after successful delivery so that I can confirm delivery.
- **US-3**: As an SRE, I want failed deliveries to be retried with exponential backoff so that transient provider failures are handled automatically.
- **US-4**: As an SRE, I want permanently failed notifications to land in a dead-letter queue and have a `failureReason` recorded so that I can investigate and take action.
- **US-5**: As a Platform Developer, I want delivery to be idempotent so that reprocessing a `NotificationCreatedEvent` does not send duplicate messages.

## 6. Requirements

### Functional Requirements

- **FR-1**: A `DeliveryHandler` Kafka consumer MUST listen to `hermes.notification.created` topic and trigger delivery for each `NotificationCreatedEvent`.
- **FR-2**: The `DeliveryHandler` MUST reconstitute the notification aggregate from DynamoDB Event Store to obtain the full aggregate state.
- **FR-3**: The `DeliveryHandler` MUST resolve the correct provider adapter based on `NotificationType` using a `ProviderAdapterRegistry`.
- **FR-4**: On successful provider response, the `DeliveryHandler` MUST call `aggregate.markAsSent(shippingReceipt)`, persist the new event to DynamoDB, and publish a `NotificationSentEvent` to Kafka topic `hermes.notification.sent`.
- **FR-5**: On provider failure, the `DeliveryHandler` MUST publish a `NotificationDeliveryFailedEvent` to Kafka topic `hermes.notification.delivery-failed` with the failure reason.
- **FR-6**: The `DeliveryHandler` MUST implement idempotency: if the aggregate already has `sentAt != null` (i.e., already sent), skip delivery and return success.
- **FR-7**: A new `NotificationDeliveryFailedEvent` domain event MUST be added to the sealed hierarchy.
- **FR-8**: Retry policy MUST be configurable via `application.properties`:
  - `hermes.delivery.max-retries=3`
  - `hermes.delivery.initial-backoff-ms=1000`
  - `hermes.delivery.backoff-multiplier=2.0`
- **FR-9**: After max retries exhausted, the message MUST be sent to the Kafka dead-letter queue (SmallRye `failure-strategy=dead-letter-queue`).

### Non-Functional Requirements

- **NFR-1**: Delivery latency (from event creation to provider call) MUST be < 5 s p95 under normal load.
- **NFR-2**: All error paths MUST use Arrow-kt `Either<BaseError, T>`.
- **NFR-3**: The `DeliveryHandler` MUST be annotated `@Blocking` for blocking I/O (DynamoDB reads, provider HTTP calls).
- **NFR-4**: Provider adapter calls MUST have configurable timeouts (default 10 s).
- **NFR-5**: Structured logging with correlation ID (aggregate ID) for all delivery steps.

## 7. Acceptance Criteria

### US-1: Auto-Delivery

- **Given** a notification is created via `POST /notifications/email`; **When** the `NotificationCreatedEvent` is consumed by `DeliveryHandler`; **Then** the Email provider adapter is invoked with the notification content.

### US-2: Sent Status

- **Given** the provider returns success; **When** the `NotificationSentEvent` is published and projected; **Then** `GET /notifications/{id}` returns `sentAt` with a non-null timestamp.

### US-3: Retry

- **Given** the provider returns a transient error (e.g., HTTP 503); **When** the `DeliveryHandler` throws a `RuntimeException`; **Then** SmallRye retries the message delivery according to the configured backoff policy.

### US-4: Permanent Failure

- **Given** retries are exhausted; **When** the message is nacked for the last time; **Then** the message lands in the dead-letter topic and a `NotificationDeliveryFailedEvent` is published.

### US-5: Idempotency

- **Given** a notification has already been sent (`sentAt` is set); **When** the same `NotificationCreatedEvent` is reprocessed; **Then** no duplicate provider call is made and no error is returned.

## 8. Out of Scope

- Provider adapter implementations (covered by Feature F4).
- Webhook callbacks to consuming services on delivery status changes.
- Delivery scheduling (send-at-time / delayed delivery).
- Priority queues / lane-based delivery ordering.
- Circuit breaker pattern for provider calls (potential future enhancement).
