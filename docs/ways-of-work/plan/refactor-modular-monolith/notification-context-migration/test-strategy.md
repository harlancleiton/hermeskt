# Test Strategy: Notification Context Migration

## 1. Test Strategy Overview
- **Testing Scope**: The relocation of all Notification, Producer, Consumer, REST Controller, and DB/Kafka configuration code into `br.com.olympus.hermes.notification`. This represents the largest physical code motion in the epic.
- **Quality Objectives**: Ensure that Notification dispatching via REST, internal Domain Event publishing via Kafka, asynchronous Projecting to MongoDB, and Delivery to simulated External Providers all continue functioning identically.
- **Risk Assessment**: High risk of breaking Kafka consumers (`@Incoming` annotations), DynamoDB persistence (`@DynamoDbBean`), and Panache MongoDB projection scanning if packages aren't reflected properly in application properties or indexers. 
- **Test Approach**: Execution of the robust suite of unit and integration tests (REST-Assured, MockK, TestContainers).

## 2. ISTQB Framework Implementation

### Test Design Techniques Selection
- **Experience-Based Testing**: Monitoring the Quarkus test boot logs to verify Kafka testcontainer topology creation and DynamoDB tables creation against the new package-bound entity models.

### Test Types Coverage Matrix
- **Structural Testing**: Confirming total isolation. The obsolete `br.com.olympus.hermes.core` should be completely empty and deleted.
- **Functional Testing**: End-to-end dispatch of Notification entities via HTTP POST, and observing successful state transitions (Created -> Sent/Failed).
- **Change-Related Testing (Regression)**: Total execution of `NotificationControllerTest`, `WhatsAppProviderAdapterTest`, `CreateNotificationHandlerTest`, etc.

## 3. ISO 25010 Quality Characteristics Assessment
- **Maintainability**: Critical priority. The application finally aligns completely with the Modular Monolith ideal.
- **Functional Suitability**: High priority. Complete behavioral freeze required.

## 4. Test Environment and Data Strategy
- **Environment**: Standard local development environment and CI/CD pipeline using Java 21/Maven.
- **Data**: Mock external provider endpoints and test containers for internal infra.
- **Tools**: JUnit 5, REST-Assured, Kafka/MongoDB/DynamoDB TestContainers.

---

# Test Issues Checklist

## Test Level Issues Creation
- [x] **Regression Test Issues**: Execute all tests using `./mvnw test` and verify that the TestContainers spin up without missing bean/entity exceptions.
- [x] **Integration Test Issues**: Verify that the Kafka loops (`hermes-notification-created` -> `NotificationCreatedProjector` -> `NotificationView` DB upsert) succeed.

## Test Dependencies Documentation
- **Implementation Dependencies**: Depends strictly on the package move execution. Do not execute tests until the legacy `core` package is deleted to ensure no dirty classpath shadowing.

## Quality Assurance Plan

### Quality Gates Validation
**Entry Criteria:**
- All Notification-specific classes moved to `br.com.olympus.hermes.notification`.
- Obsolete root directories deleted.
- `TemplateContext` and `SharedKernel` already securely in place.

**Exit Criteria:**
- `mvnw clean verify` succeeds.
- No class in `br.com.olympus.hermes` resides outside of `shared`, `template`, `notification`, or standard configuration roots like `config`.
