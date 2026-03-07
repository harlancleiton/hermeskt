# Epic: mount-notifications

## 1. Epic Name
mount-notifications

## 2. Goal

**Problem:** 
The application needs a reliable way to notify users about critical asynchronous domain events (like requesting a 2FA code or successfully resetting a password). Currently, there's no dedicated system to bridge the gap between business events occurring in Kafka and the actual formatted notification being dispatched.

**Solution:** 
Build an event-driven system (the "mount notifications" feature) that listens over Kafka for specific occurrences. Upon receiving a message, the system will determine the correct notification template, "mount" (populate) the template with data from the event (like the 2FA code or the user's name), and trigger the dispatch of the notification.

**Impact:** 
This ensures a decoupled architecture where domain services only need to emit events without worrying about formatting or delivery details. It directly improves user experience, security, and engagement through timely delivery of transactional messages.

## 3. User Personas

- **End User:** Needs to receive transactional notifications promptly (e.g., 2FA codes to log in, password reset instructions) without delays.
- **System Administrator / Operator:** Needs observability to monitor notification delivery rates, latency, and failures (e.g., dead letter queues).

## 4. High-Level User Journeys

1. **Authentication / Security Event:** A user performs an action (e.g., requesting a 2FA code). The authentication domain publishes a `User2FACodeRequested` event to a Kafka topic.
2. **Event Consumption:** The `mount-notifications` service consumes this event.
3. **Template Mounting:** The service identifies the mapped template for the event, replaces placeholders (e.g., `{{code}}`, `{{user.name}}`) with the event data, and prepares the notification payload.
4. **Dispatch:** The service sends the final mounted notification via the delivery layer (Email/SMS) to the target user.

## 5. Business Requirements

**Functional Requirements:**
- The system must consume domain events from configured Kafka topics reliably.
- The system must be able to map a specific Kafka event type to a corresponding notification template.
- The system must populate (mount) the notification template with dynamic payload data from the consumed Kafka event.
- The system must be able to send the mounted notification to the user through their preferred or predefined channel (Email/SMS).
- The system must handle message deserialization errors gracefully.

**Non-Functional Requirements:**
- **Reliability:** At-least-once processing guarantee for Kafka messages to prevent missed notifications.
- **Resilience:** The system must implement Dead-Letter Queues (DLQ) for events that fail to process after a defined number of retries.
- **Performance:** End-to-end latency from event emission to notification dispatch should be under 5 seconds for critical transactional emails (like 2FA).
- **Scalability:** The Kafka consumer groups should be partitioned and scalable horizontally to handle workload spikes.
- **Observability:** Key metrics (e.g., consumption rate, template mount success, dispatch success rate) must be trackable via OpenTelemetry.

## 6. Success Metrics

- **Delivery Latency:** 95% of security/transactional notifications dispatched within 5 seconds of the source event.
- **Delivery Success Rate:** > 99.9% of correctly formatted domain events result in a successful dispatch.
- **DLQ Rate:** Less than 0.1% of consumed events fail fully and land in the DLQ.

## 7. Out of Scope

- Creating or designing the content of the notification templates themselves (this is handled by the Template context).
- Implementing the downstream third-party integrations for SMS or Email (e.g., SendGrid, Twilio); we assume a standard dispatch port is used.
- Creating the upstream domain events in other bounded contexts.

## 8. Business Value

**High:** Transactional notifications like 2FA and password resets are foundational to platform security and user access. If this system fails or is slow, users cannot access their accounts, leading to a blocked user journey, frustration, and increased support requests.
