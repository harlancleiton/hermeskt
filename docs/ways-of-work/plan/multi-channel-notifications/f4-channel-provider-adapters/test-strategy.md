# Test Strategy: Channel Provider Adapters

## 1. Test Strategy Overview

### Testing Scope

- `EmailProviderAdapter` (SMTP / SES)
- `SmsProviderAdapter` (Twilio / SNS)
- `PushProviderAdapter` (FCM)
- `WhatsAppProviderAdapter` (WhatsApp Business Cloud API)
- `ProviderAdapterRegistry` auto-discovery
- Configuration injection (`@ConfigProperty`)
- Timeout handling per adapter
- Error translation to `DeliveryError`

### Quality Objectives

- ≥ 80 % line coverage per adapter
- 100 % acceptance criteria validation
- All adapters correctly translate provider errors to `Either.Left(DeliveryError(...))`
- No provider secrets leaked in logs

### Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| External provider unavailable during CI | High | Medium | Mock providers in unit tests; use test sandboxes / embedded servers in IT |
| Credential misconfiguration | Medium | High | Startup validation + clear error messages; config test |
| Provider SDK breaking changes | Low | Medium | Pin dependency versions; contract tests |
| Timeout too short for slow providers | Medium | Medium | Configurable timeout with sensible defaults; boundary tests |
| Secret leakage in logs | Low | High | Never log credential values; review test for sensitive data in log output |

## 2. Test Design Techniques

### Equivalence Partitioning per Adapter

| Scenario | Expected |
|----------|----------|
| Valid notification + provider returns success | `Either.Right(ProviderReceipt)` |
| Valid notification + provider returns error (4xx) | `Either.Left(DeliveryError)` |
| Valid notification + provider timeout | `Either.Left(DeliveryError("timeout"))` |
| Valid notification + network error | `Either.Left(DeliveryError("connection"))` |
| Wrong notification type | Should not reach adapter (`supports()` returns false) |

### Boundary Value Analysis

| Boundary | Test Values |
|----------|------------|
| Timeout | 1 ms (immediate timeout), 10000 ms (default), provider responds at 9999 ms (just in time) |
| Content length | 1 char (min), max channel limit (e.g., 160 chars SMS) |
| Device token | Valid FCM token format, invalid token (provider rejects) |

## 3. Test Plan

### 3.1 Unit Tests (MockK — no external calls)

#### EmailProviderAdapterTest
- `send should return ProviderReceipt with message ID on success`
- `send should return DeliveryError when Transport.send throws`
- `send should map EmailNotification fields to MimeMessage correctly`
- `supports should return true for EMAIL, false for others`

#### SmsProviderAdapterTest
- `send should return ProviderReceipt with SID on success`
- `send should return DeliveryError when Twilio API throws`
- `send should map SmsNotification fields correctly`
- `supports should return true for SMS, false for others`

#### PushProviderAdapterTest
- `send should return ProviderReceipt with FCM message ID on success`
- `send should return DeliveryError when FirebaseMessaging throws`
- `send should map PushNotification fields to FCM Message correctly`
- `send should include data map in FCM payload`
- `supports should return true for PUSH, false for others`

#### WhatsAppProviderAdapterTest
- `send should return ProviderReceipt with message ID on success`
- `send should return DeliveryError on HTTP error response`
- `send should return DeliveryError on timeout`
- `send should set Authorization header with Bearer token`
- `send should map WhatsAppNotification fields to API body correctly`
- `supports should return true for WHATSAPP, false for others`

#### ProviderAdapterRegistryTest
- `should discover all four adapters via CDI`
- `getAdapter should return correct adapter for each NotificationType`
- `getAdapter should return ProviderAdapterNotFoundError for unregistered type`

### 3.2 Integration Tests

#### EmailProviderAdapterIT
- Use embedded SMTP server (e.g., GreenMail `com.icegreen:greenmail`)
- `should deliver email to embedded SMTP and verify received message`
- `should return DeliveryError for unreachable SMTP host`

#### SmsProviderAdapterIT
- Use Twilio test credentials (test AccountSid, test AuthToken, test phone numbers)
- `should send SMS via Twilio test API`
- `should return DeliveryError for invalid phone number`

#### PushProviderAdapterIT
- Use Firebase emulator or mock HTTP server intercepting FCM calls
- `should send push via FCM and receive message ID`
- `should return DeliveryError for invalid device token`

#### WhatsAppProviderAdapterIT
- Use WireMock to stub WhatsApp Cloud API
- `should send message and parse response message ID`
- `should return DeliveryError on 401 Unauthorized`
- `should return DeliveryError on request timeout`

### 3.3 Contract Tests

For each adapter:
- `ProviderReceipt.receiptId is non-blank on success`
- `ProviderReceipt.provider matches expected provider name`
- `DeliveryError.message contains meaningful context on failure`
- `DeliveryError.cause contains original exception`

### 3.4 Configuration Tests
- `should fail startup gracefully if required credentials are missing`
- `should use default timeout when not configured`
- `should use custom timeout when configured`

## 4. Quality Gates

### Entry Criteria
- `NotificationProviderAdapter` port interface defined (from F3)
- `ProviderReceipt` and `DeliveryError` domain types defined
- Provider SDK dependencies added to `pom.xml`

### Exit Criteria
- All unit and integration tests pass
- ≥ 80 % line coverage per adapter
- No secrets in test log output
- Each adapter handles timeout correctly
- Code review approved

## 5. Test Estimation

| Test Category | Estimated Effort |
|--------------|-----------------|
| Unit tests (4 adapters + registry) | 2 SP |
| Integration tests (4 adapters with mocks/embedded) | 3 SP |
| Contract tests | 0.5 SP |
| Configuration tests | 0.5 SP |
| **Total** | **6 SP** |
