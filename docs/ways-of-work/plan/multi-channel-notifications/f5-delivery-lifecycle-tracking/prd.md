# Feature PRD: Delivery Lifecycle Tracking

## 1. Feature Name

**Delivery Lifecycle Tracking**

## 2. Epic

- [Epic PRD](../epic.md)
- [Architecture Spec](../arch.md)

## 3. Goal

### Problem

After a notification is created, there is no visibility into its delivery status. The `NotificationView` in MongoDB has `sentAt`, `deliveryAt`, and `seenAt` fields but they are never populated by an automated process. Platform developers cannot query the current delivery state of a notification or filter notifications by status. There is no `NotificationDeliveryFailedEvent` to track permanent failures.

### Solution

Extend the read model and query API to provide full delivery lifecycle tracking:

1. New projectors (`NotificationSentEventHandler`, `NotificationFailedEventHandler`) that update `NotificationView` when lifecycle events arrive from Kafka.
2. Add `status` (derived: PENDING, SENT, DELIVERED, FAILED) and `failureReason` fields to `NotificationView`.
3. Enhance query endpoints: `GET /notifications/{id}` returns full lifecycle data; new `GET /notifications` endpoint supports pagination, filtering by `type`, `status`, and date range.

### Impact

- Platform developers get real-time delivery status visibility.
- Operations teams can identify and triage failed deliveries.
- Enables SLA monitoring and delivery analytics dashboards.

## 4. User Personas

- **Platform Developer**: Queries notification status to confirm delivery or diagnose failures.
- **Operations / SRE**: Filters notifications by status to find failures and monitor delivery health.

## 5. User Stories

- **US-1**: As a Platform Developer, I want `GET /notifications/{id}` to return `sentAt`, `deliveryAt`, `seenAt`, `status`, and `failureReason` so that I have full delivery visibility.
- **US-2**: As a Platform Developer, I want to list notifications via `GET /notifications` with pagination so that I can browse notification history.
- **US-3**: As a Platform Developer, I want to filter notifications by `type` (EMAIL, SMS, PUSH, WHATSAPP) so that I can view channel-specific notifications.
- **US-4**: As a Platform Developer, I want to filter notifications by `status` (PENDING, SENT, DELIVERED, FAILED) so that I can find problematic notifications.
- **US-5**: As a Platform Developer, I want to filter notifications by date range (`createdFrom`, `createdTo`) so that I can narrow results to a time window.
- **US-6**: As an SRE, I want the `NotificationView` to be updated with `sentAt` when a `NotificationSentEvent` is projected so that delivery timing is tracked.
- **US-7**: As an SRE, I want the `NotificationView` to be updated with `failureReason` when a `NotificationDeliveryFailedEvent` is projected so that failures are recorded.

## 6. Requirements

### Functional Requirements

- **FR-1**: `NotificationView` MUST include:
  - `status: String` — derived from lifecycle: `PENDING` (default), `SENT` (sentAt set), `DELIVERED` (deliveryAt set), `FAILED` (failureReason set).
  - `failureReason: String?` — null unless delivery failed.
- **FR-2**: `NotificationSentEventHandler` projector MUST update `NotificationView.sentAt` and set `status = SENT` when consuming a `NotificationSentEvent`.
- **FR-3**: `NotificationFailedEventHandler` projector MUST update `NotificationView.failureReason` and set `status = FAILED` when consuming a `NotificationDeliveryFailedEvent`.
- **FR-4**: Existing `NotificationDeliveredEvent` projector (if delivery confirmation comes from provider webhook in the future) MUST set `status = DELIVERED` and `deliveryAt`.
- **FR-5**: `GET /notifications/{id}` MUST return the full `NotificationView` including all lifecycle fields and `status`.
- **FR-6**: `GET /notifications` MUST support:
  - Pagination: `page` (default 0), `size` (default 20, max 100).
  - Filtering: `type` (optional), `status` (optional), `createdFrom` (optional ISO date), `createdTo` (optional ISO date).
  - Sorting: by `createdAt` descending (default).
- **FR-7**: The list endpoint MUST return a paginated response with `content`, `page`, `size`, `totalElements`, `totalPages`.
- **FR-8**: MongoDB indexes MUST be created for efficient queries: compound index on `(type, status, createdAt)`.

### Non-Functional Requirements

- **NFR-1**: List query MUST complete in < 100 ms p99 for pages of 20 documents.
- **NFR-2**: All error paths MUST use Arrow-kt `Either<BaseError, T>`.
- **NFR-3**: All endpoints MUST be documented via SmallRye OpenAPI annotations.
- **NFR-4**: Projectors MUST be idempotent.

## 7. Acceptance Criteria

### US-1: Get Notification with Lifecycle

- **Given** a notification that has been sent; **When** `GET /notifications/{id}` is called; **Then** the response includes `sentAt`, `status = SENT`, and all other lifecycle fields.
- **Given** a notification that has failed; **When** `GET /notifications/{id}` is called; **Then** the response includes `failureReason` and `status = FAILED`.

### US-2: List Notifications

- **Given** 50 notifications exist; **When** `GET /notifications?page=0&size=20` is called; **Then** 20 notifications are returned with `totalElements = 50`, `totalPages = 3`.

### US-3: Filter by Type

- **Given** notifications of types EMAIL and SMS exist; **When** `GET /notifications?type=EMAIL` is called; **Then** only EMAIL notifications are returned.

### US-4: Filter by Status

- **Given** PENDING and SENT notifications exist; **When** `GET /notifications?status=SENT` is called; **Then** only SENT notifications are returned.

### US-5: Filter by Date Range

- **Given** notifications created on various dates; **When** `GET /notifications?createdFrom=2025-01-01&createdTo=2025-01-31` is called; **Then** only notifications created within that range are returned.

### US-6 & US-7: Projector Updates

- **Given** a `NotificationSentEvent` is consumed; **When** the projector runs; **Then** the `NotificationView.sentAt` is set and `status = SENT`.
- **Given** a `NotificationDeliveryFailedEvent` is consumed; **When** the projector runs; **Then** `NotificationView.failureReason` is set and `status = FAILED`.

## 8. Out of Scope

- Real-time delivery status push (WebSocket / SSE) to consuming services.
- Provider webhook integration for delivery confirmation (future).
- Delivery analytics / reporting dashboards.
- Notification deletion or archival.
