# Feature PRD: Channel Provider Adapters

## 1. Feature Name

**Channel Provider Adapters**

## 2. Epic

- [Epic PRD](../epic.md)
- [Architecture Spec](../arch.md)

## 3. Goal

### Problem

The Delivery Pipeline (F3) requires concrete provider adapters to send notifications through external APIs. Without these adapters, the pipeline has no way to actually deliver messages to end-users. Each channel has a distinct external API with different authentication, payload formats, and error semantics.

### Solution

Implement four channel-specific provider adapters behind the `NotificationProviderAdapter` output port interface:

1. **Email** — SMTP via Jakarta Mail or AWS SES SDK.
2. **SMS** — Twilio SMS API or AWS SNS.
3. **Push** — Firebase Cloud Messaging (FCM) Admin SDK.
4. **WhatsApp** — WhatsApp Business API (Cloud API) via HTTP client.

Each adapter is an `@ApplicationScoped` CDI bean implementing the port, registered automatically via the `ProviderAdapterRegistry`. Provider credentials are injected via Quarkus config / environment variables.

### Impact

- Enables actual message delivery for all four channels.
- Provider implementations are swappable (e.g., swap SES for SendGrid) without touching domain or application code.
- Centralised credential management and timeout configuration.

## 4. User Personas

- **Platform Developer**: Expects notifications to reach end-users after creation.
- **Operations / SRE**: Configures provider credentials and monitors delivery metrics.

## 5. User Stories

- **US-1**: As a Platform Developer, I want email notifications to be delivered via SMTP/SES so that recipients receive emails.
- **US-2**: As a Platform Developer, I want SMS notifications to be delivered via a gateway so that recipients receive text messages.
- **US-3**: As a Platform Developer, I want push notifications to be delivered via FCM so that recipients receive mobile/web push alerts.
- **US-4**: As a Platform Developer, I want WhatsApp notifications to be delivered via the WhatsApp Business API so that recipients receive WhatsApp messages.
- **US-5**: As an SRE, I want provider credentials to be configurable via environment variables so that secrets are not hardcoded.
- **US-6**: As an SRE, I want each provider adapter to have a configurable timeout so that slow providers do not block the delivery pipeline indefinitely.
- **US-7**: As an SRE, I want structured logs for every provider call (success/failure) with correlation ID so that I can trace delivery issues.

## 6. Requirements

### Functional Requirements

- **FR-1**: Each adapter MUST implement `NotificationProviderAdapter` with `send(notification): Either<BaseError, ProviderReceipt>` and `supports(type): Boolean`.
- **FR-2**: The **Email adapter** MUST:
  - Send emails via SMTP (Jakarta Mail) or AWS SES SDK.
  - Map `EmailNotification.from`, `to`, `subject`, `content` to the email payload.
  - Return a `ProviderReceipt` with the provider message ID.
- **FR-3**: The **SMS adapter** MUST:
  - Send SMS via Twilio API or AWS SNS.
  - Map `SmsNotification.from`, `to`, `content` to the SMS payload.
  - Return a `ProviderReceipt` with the provider message SID/ID.
- **FR-4**: The **Push adapter** MUST:
  - Send push notifications via Firebase Admin SDK (FCM).
  - Map `PushNotification.deviceToken`, `title`, `content`, `data` to the FCM message.
  - Return a `ProviderReceipt` with the FCM message ID.
- **FR-5**: The **WhatsApp adapter** MUST:
  - Send messages via WhatsApp Business Cloud API (HTTP POST).
  - Map `WhatsAppNotification.from`, `to`, `templateName`, `content`, `payload` to the API request body.
  - Return a `ProviderReceipt` with the WhatsApp message ID.
- **FR-6**: All adapters MUST wrap external API calls in `Either.catch { }.mapLeft { DeliveryError(...) }` to translate provider exceptions into domain errors.
- **FR-7**: Provider credentials MUST be injected via `@ConfigProperty` from `application.properties` or environment variables:
  - Email: `hermes.provider.email.smtp-host`, `smtp-port`, `smtp-username`, `smtp-password` (or SES region + credentials)
  - SMS: `hermes.provider.sms.account-sid`, `auth-token`, `from-number`
  - Push: `hermes.provider.push.fcm-credentials-path`
  - WhatsApp: `hermes.provider.whatsapp.api-url`, `access-token`, `phone-number-id`
- **FR-8**: Each adapter MUST respect a configurable timeout: `hermes.provider.{channel}.timeout-ms` (default 10000).

### Non-Functional Requirements

- **NFR-1**: Adapter implementations MUST live under `shared/infrastructure/providers/`.
- **NFR-2**: Adapters MUST NOT contain business logic — they are pure infrastructure translation layers.
- **NFR-3**: All provider HTTP calls MUST use a Quarkus REST client or `java.net.http.HttpClient` with connection pooling.
- **NFR-4**: Each adapter MUST log the provider request/response at DEBUG level and errors at ERROR level with correlation ID.
- **NFR-5**: Provider secrets MUST never appear in logs.

## 7. Acceptance Criteria

### US-1: Email Delivery

- **Given** an `EmailNotification` aggregate; **When** the Email adapter `send()` is called; **Then** an email is sent via SMTP/SES and a `ProviderReceipt` with message ID is returned.
- **Given** invalid SMTP credentials; **When** `send()` is called; **Then** `Either.Left(DeliveryError(...))` is returned.

### US-2: SMS Delivery

- **Given** an `SmsNotification` aggregate; **When** the SMS adapter `send()` is called; **Then** an SMS is sent via the gateway and a `ProviderReceipt` with message SID is returned.

### US-3: Push Delivery

- **Given** a `PushNotification` aggregate; **When** the Push adapter `send()` is called; **Then** a push notification is sent via FCM and a `ProviderReceipt` with message ID is returned.

### US-4: WhatsApp Delivery

- **Given** a `WhatsAppNotification` aggregate; **When** the WhatsApp adapter `send()` is called; **Then** a WhatsApp message is sent via the Business API and a `ProviderReceipt` with message ID is returned.

### US-5: Credentials

- **Given** provider credentials are set via environment variables; **When** the adapter is initialised; **Then** it connects successfully using those credentials.

### US-6: Timeout

- **Given** the provider does not respond within the configured timeout; **When** `send()` is called; **Then** `Either.Left(DeliveryError("Provider timeout"))` is returned.

### US-7: Logging

- **Given** any provider call; **When** it completes; **Then** a structured log entry is written with aggregate ID, provider name, success/failure, and latency.

## 8. Out of Scope

- Provider-specific retry logic (handled by the Delivery Pipeline's Kafka retry).
- Multi-provider fallback (e.g., try SES, then SendGrid).
- Provider health checks / circuit breakers.
- Rich media email (HTML rendering, attachments).
- WhatsApp media messages.
