# Feature PRD: Push Notification Channel

## 1. Feature Name

**Push Notification Channel**

## 2. Epic

- [Epic PRD](../epic.md)
- [Architecture Spec](../arch.md)

## 3. Goal

### Problem

Hermes currently supports Email, SMS, and WhatsApp channels but lacks Push Notification support. Mobile and web applications cannot leverage Hermes to reach users via native push notifications (iOS APNs, Android FCM, Web Push). Teams must integrate directly with FCM/APNs, duplicating logic across services.

### Solution

Add a complete Push Notification channel to Hermes following the established aggregate pattern: `PushNotification` entity, `PushNotificationFactory`, `PushNotificationCreatedEvent`, command, request DTO, REST endpoint (`POST /notifications/push`), and projector update. Introduce a `DeviceToken` value object for recipient targeting.

### Impact

- Enables mobile/web engagement through a unified API.
- Eliminates duplicated FCM/APNs integration across consuming services.
- Completes the four-channel notification matrix defined in the epic.

## 4. User Personas

- **Platform Developer**: Triggers push notifications via REST API.
- **End-User (Recipient)**: Receives push notifications on mobile/web devices.

## 5. User Stories

- **US-1**: As a Platform Developer, I want to send a push notification via `POST /notifications/push` so that I can reach users on their mobile/web devices.
- **US-2**: As a Platform Developer, I want push notification creation to validate the device token, title, and body so that invalid requests are rejected with clear error messages.
- **US-3**: As a Platform Developer, I want the push notification to appear in `GET /notifications/{id}` so that I can track its lifecycle.
- **US-4**: As a Platform Developer, I want to provide a `payload` map with custom data so that the receiving app can handle deep links or custom actions.

## 6. Requirements

### Functional Requirements

- **FR-1**: The system MUST accept `POST /notifications/push` with fields: `deviceToken`, `title`, `body`, `payload` (optional), and `data` (optional key-value map for FCM/APNs custom data).
- **FR-2**: The system MUST validate `deviceToken` via a `DeviceToken` value object (non-blank, max 4096 chars).
- **FR-3**: The system MUST validate `title` (non-blank, max 255 chars) and `body` (non-blank, max 4096 chars).
- **FR-4**: The system MUST create a `PushNotification` aggregate that extends `Notification` and raises a `PushNotificationCreatedEvent`.
- **FR-5**: The `PushNotificationCreatedEvent` MUST be published to Kafka and projected into `NotificationView` with `type = PUSH`.
- **FR-6**: The `NotificationView` MUST store push-specific fields: `deviceToken`, `title`.
- **FR-7**: The `NotificationType.PUSH` factory MUST be registered in `NotificationFactoryRegistry`.

### Non-Functional Requirements

- **NFR-1**: Validation MUST use `zipOrAccumulate` to accumulate all errors.
- **NFR-2**: All error paths MUST return `Either<BaseError, T>`.
- **NFR-3**: The endpoint MUST be documented via SmallRye OpenAPI annotations.
- **NFR-4**: The implementation MUST follow the existing Hexagonal / DDD package structure.

## 7. Acceptance Criteria

### US-1: Send Push Notification

- **Given** a valid request with `deviceToken`, `title`, `body`; **When** `POST /notifications/push` is called; **Then** the system returns `201 Created` with a `NotificationResponse` containing `type = PUSH`.
- **Given** the notification is created; **When** the event is consumed by the projector; **Then** a `NotificationView` document with `type = PUSH` exists in MongoDB.

### US-2: Validation

- **Given** a request with a blank `deviceToken`; **When** `POST /notifications/push` is called; **Then** the system returns `400` with an error listing the invalid field.
- **Given** a request with blank `title` AND blank `body`; **When** `POST /notifications/push` is called; **Then** the system returns `400` with errors for both fields (accumulated).

### US-3: Query

- **Given** a push notification exists; **When** `GET /notifications/{id}` is called; **Then** the response includes `type = PUSH`, `deviceToken`, and `title`.

### US-4: Custom Data

- **Given** a request with a `data` map; **When** the notification is created; **Then** the `data` map is persisted in the aggregate and projected into the view.

## 8. Out of Scope

- Actual delivery to FCM/APNs (covered by Feature F4 — Provider Adapters).
- Topic-based push (sending to a topic instead of a device token).
- Multi-device push (sending to multiple tokens in a single request).
- Silent / background push notifications.
