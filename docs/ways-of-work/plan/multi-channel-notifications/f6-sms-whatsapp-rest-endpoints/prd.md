# Feature PRD: SMS & WhatsApp REST Endpoints

## 1. Feature Name

**SMS & WhatsApp REST Endpoints**

## 2. Epic

- [Epic PRD](../epic.md)
- [Architecture Spec](../arch.md)

## 3. Goal

### Problem

The Hermes REST API currently only exposes `POST /notifications/email`. The SMS and WhatsApp channels have full domain support (aggregates, factories, events, commands) but no REST endpoint to trigger them. Platform developers cannot create SMS or WhatsApp notifications via the API.

### Solution

Add two new REST endpoints — `POST /notifications/sms` and `POST /notifications/whatsapp` — with their corresponding request DTOs. Each endpoint maps the incoming HTTP request to the existing `CreateNotificationCommand.Sms` / `CreateNotificationCommand.WhatsApp` and delegates to the shared `CreateNotificationHandler`. This is a thin REST layer addition with no changes to the domain or application layers.

### Impact

- Completes the REST API surface for all pre-existing channels.
- Enables Platform Developers to trigger SMS and WhatsApp notifications via the same API pattern used for Email.
- Unblocks end-to-end testing of the delivery pipeline for SMS and WhatsApp.

## 4. User Personas

- **Platform Developer**: Sends SMS and WhatsApp notifications via REST API.

## 5. User Stories

- **US-1**: As a Platform Developer, I want to send an SMS notification via `POST /notifications/sms` so that I can reach users via text message.
- **US-2**: As a Platform Developer, I want to send a WhatsApp notification via `POST /notifications/whatsapp` so that I can reach users via WhatsApp.
- **US-3**: As a Platform Developer, I want clear validation errors when I submit invalid SMS or WhatsApp requests so that I can fix my payloads.

## 6. Requirements

### Functional Requirements

- **FR-1**: `POST /notifications/sms` MUST accept a JSON body with fields: `content`, `payload` (optional), `from` (short code, UInt), `to` (phone number string).
- **FR-2**: `POST /notifications/whatsapp` MUST accept a JSON body with fields: `content`, `payload` (optional), `from` (phone string), `to` (phone string), `templateName` (WhatsApp Business API template name).
- **FR-3**: Both endpoints MUST map the request to the corresponding `CreateNotificationCommand` subtype and delegate to `CreateNotificationHandler`.
- **FR-4**: Both endpoints MUST return `201 Created` with a `NotificationResponse` on success.
- **FR-5**: Both endpoints MUST return `400 Bad Request` with accumulated validation errors on invalid input.
- **FR-6**: Both endpoints MUST return `500 Internal Server Error` for server-side failures.
- **FR-7**: Both request DTOs MUST include an optional `templateName: String?` field for integration with the Template Engine (F2) when available.

### Non-Functional Requirements

- **NFR-1**: Endpoints MUST be documented via SmallRye OpenAPI annotations.
- **NFR-2**: Follow the same controller pattern as the existing `createEmailNotification` method.
- **NFR-3**: No changes to domain or application layers — purely REST/DTO additions.

## 7. Acceptance Criteria

### US-1: SMS Endpoint

- **Given** a valid SMS request; **When** `POST /notifications/sms` is called; **Then** `201 Created` is returned with `type = SMS` in the response.
- **Given** a request with an invalid phone number; **When** `POST /notifications/sms` is called; **Then** `400` is returned with `InvalidPhoneError`.

### US-2: WhatsApp Endpoint

- **Given** a valid WhatsApp request; **When** `POST /notifications/whatsapp` is called; **Then** `201 Created` is returned with `type = WHATSAPP` in the response.
- **Given** a request with blank `templateName`; **When** `POST /notifications/whatsapp` is called; **Then** `400` is returned with `EmptyContentError("templateName")`.

### US-3: Validation

- **Given** a request with multiple invalid fields; **When** either endpoint is called; **Then** `400` is returned with all validation errors accumulated.

## 8. Out of Scope

- Changes to domain entities, factories, or events (already implemented).
- Delivery logic (covered by F3 and F4).
- Template resolution integration (covered by F2; DTOs include `templateName` field for future use).
