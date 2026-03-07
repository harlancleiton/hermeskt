# Feature PRD: f3-notification-dispatch-flow

## 1. Feature Name
f3-notification-dispatch-flow

## 2. Epic
[mount-notifications](../epic.md)  
[Architecture](../arch.md)

## 3. Goal
**Problem:** Once a domain event is received and its template is successfully mounted with dynamic data, the system needs to record this intent and physically dispatch the message to the user via an external provider (Email or SMS).
**Solution:** Implement the core CQRS Command flow (`CreateNotificationCommand`). This flow will create a `Notification` aggregate, persist it to the DynamoDB Event Store, and immediately pass the mounted payload to an output port for provider delivery.
**Impact:** Completes the end-to-end journey of the epic, ensuring users actually receive the notifications triggered by upstream events.

## 4. User Personas
- **End User:** Receives the final email or SMS.
- **System Administrator / Operator:** Needs to monitor dispatch success rates and view notification history for auditing (via the read model).

## 5. User Stories
- As a Hermes system component, I want to create and durably store a record of every notification sent so that we have an audit trail of user communication.
- As a Hermes system component, I want to physically dispatch the compiled notification to an external provider (like Amazon SES or Twilio) so that the user receives the message.
- As a Hermes system operator, I want failed dispatches (e.g., due to an external provider outage) to be handled gracefully (e.g., marked as `Failed` in the DB or retried) so that we don't silently lose messages.

## 6. Requirements

**Functional Requirements:**
- Implement a `CreateNotificationCommand` and corresponding `CreateNotificationHandler`.
- The handler must create a new `Notification` aggregate (extending `AggregateRoot`).
- The aggregate must generate a `NotificationCreated` domain event upon creation.
- The handler must save the aggregate to the DynamoDB event store via `aggregate.commit()`.
- The system must define an output port (e.g., `NotificationProviderPort`) to send the message.
- The system must project the `NotificationCreated` event into a MongoDB read model (`NotificationView`) for auditing/UI display.

**Non-Functional Requirements:**
- **Reliability:** The aggregate must be saved *before* (or transactionally with) the external provider dispatch if possible, or at least the system must guarantee the record isn't lost if the provider drops the connection. (Standard Outbox or simple synchronous dispatch with state updates).
- **Resilience:** The provider port should implement retries (using Resilience4j) for transient HTTP errors from external services like SendGrid or Twilio.
- **Traceability:** The ID of the original Kafka event should be stored on the `Notification` aggregate to trace "Why was this sent?".

## 7. Acceptance Criteria
- [ ] `CreateNotificationCommand` and `CreateNotificationHandler` are implemented in the `application` layer.
- [ ] `Notification` aggregate and `NotificationCreated` event are implemented in the `domain` layer.
- [ ] DynamoDB repository successfully persists the aggregate events.
- [ ] Provider Port interface is defined, and a simple dummy/log implementation is provided for the infrastructure layer (actual third-party integration is usually deferred or stubbed at this stage).
- [ ] A projector (`NotificationCreatedProjector`) and view (`NotificationView`) are implemented to mirror the data to MongoDB.
- [ ] End-to-end test (or comprehensive integration test) proves an event goes from Handler -> Aggregate -> DynamoDB -> Projector -> MongoDB.

## 8. Out of Scope
- Actually integrating and paying for real SendGrid/Twilio API keys (we will build the adapter/port, but likely use a mock or logging implementation for initial rollout).
- Handling manual resends (user clicking "resend 2FA" is handled upstream, they just emit a new event).
