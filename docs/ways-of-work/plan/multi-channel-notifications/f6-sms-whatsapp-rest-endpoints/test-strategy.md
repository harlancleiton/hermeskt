# Test Strategy: SMS & WhatsApp REST Endpoints

## 1. Test Strategy Overview

### Testing Scope

- `CreateSmsNotificationRequest` DTO and `toCommand()` mapping
- `CreateWhatsAppNotificationRequest` DTO and `toCommand()` mapping
- `NotificationController.createSmsNotification()` endpoint
- `NotificationController.createWhatsAppNotification()` endpoint
- OpenAPI annotations (verified via Swagger UI or OpenAPI spec)

### Quality Objectives

- ≥ 80 % line coverage for all new DTOs and controller methods
- 100 % acceptance criteria validation
- Zero regressions on existing Email endpoint

### Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| `UInt` serialization issue in SMS `from` field | Medium | Medium | Explicit Jackson serialization test |
| WhatsApp `templateName` confused with Hermes template name | Medium | Medium | Clear field naming in DTO; doc comments |
| Existing Email endpoint regresses | Low | High | Regression test included |

## 2. Test Design Techniques

### Equivalence Partitioning

| Input | Valid Partition | Invalid Partitions |
|-------|---------------|-------------------|
| SMS `to` | Valid Brazilian phone (11 digits) | Blank, too short, too long, non-numeric |
| SMS `from` | Valid UInt short code | (Jackson handles type; invalid caught at deserialization) |
| SMS `content` | Non-blank string | Blank / empty |
| WhatsApp `from` | Valid Brazilian phone | Blank, invalid format |
| WhatsApp `to` | Valid Brazilian phone | Blank, invalid format |
| WhatsApp `templateName` | Non-blank string | Blank / empty |
| WhatsApp `content` | Non-blank string | Blank / empty |

### Decision Table: Validation Accumulation

| content blank | to invalid | templateName blank (WA) | Expected |
|:---:|:---:|:---:|:---|
| N | N | N | 201 Created |
| Y | N | N | 400 with EmptyContentError |
| N | Y | N | 400 with InvalidPhoneError |
| N | N | Y | 400 with EmptyContentError("templateName") |
| Y | Y | Y | 400 with all errors accumulated |

## 3. Test Plan

### 3.1 Unit Tests

#### CreateSmsNotificationRequestTest
- `toCommand should map content correctly`
- `toCommand should map from as UInt`
- `toCommand should map to correctly`
- `toCommand should map payload correctly`
- `toCommand should set type to SMS`

#### CreateWhatsAppNotificationRequestTest
- `toCommand should map all fields correctly`
- `toCommand should set type to WHATSAPP`
- `toCommand should map templateName correctly`

### 3.2 Integration Tests (`@QuarkusTest` + REST-Assured)

#### NotificationControllerSmsIT
- `POST /notifications/sms with valid request returns 201 with type SMS`
- `POST /notifications/sms with blank content returns 400`
- `POST /notifications/sms with invalid phone number returns 400 with InvalidPhoneError`
- `POST /notifications/sms with blank content AND invalid phone returns 400 with accumulated errors`
- `POST /notifications/sms response body contains notification id`

#### NotificationControllerWhatsAppIT
- `POST /notifications/whatsapp with valid request returns 201 with type WHATSAPP`
- `POST /notifications/whatsapp with blank content returns 400`
- `POST /notifications/whatsapp with invalid from phone returns 400`
- `POST /notifications/whatsapp with invalid to phone returns 400`
- `POST /notifications/whatsapp with blank templateName returns 400`
- `POST /notifications/whatsapp with all invalid fields returns 400 with accumulated errors`
- `POST /notifications/whatsapp response body contains notification id`

#### OpenAPI Spec Verification
- `GET /q/openapi includes /notifications/sms endpoint`
- `GET /q/openapi includes /notifications/whatsapp endpoint`

### 3.3 Regression Tests
- `POST /notifications/email still returns 201 for valid request`
- `GET /notifications/{id} still works for SMS and WhatsApp notifications`

## 4. Quality Gates

### Entry Criteria
- `CreateNotificationCommand.Sms` and `CreateNotificationCommand.WhatsApp` already implemented
- `CreateNotificationHandler` handles both command types
- Existing factories and aggregates for SMS and WhatsApp functional

### Exit Criteria
- All unit and integration tests pass
- ≥ 80 % line coverage on new code
- OpenAPI spec includes both new endpoints
- Code review approved

## 5. Test Estimation

| Test Category | Estimated Effort |
|--------------|-----------------|
| Unit tests (request DTOs) | 0.5 SP |
| Integration tests (REST endpoints) | 1.5 SP |
| Regression tests | 0.5 SP |
| **Total** | **2.5 SP** |
