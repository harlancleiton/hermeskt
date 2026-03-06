# Issue Creation Checklist: Notification Context Migration

## Pre-Creation Preparation
- [x] **Feature artifacts complete**: PRD, tech breakdown, test plan done
- [x] **Epic exists**: `refactor-modular-monolith` epic created
- [x] **Project board configured**: Kanban structure ready
- [x] **Team capacity assessed**: N/A for automated plan

## Feature Level Issues
- [ ] **Feature issue created** linking to parent epic
- [ ] **Feature dependencies identified**: Blocked by Shared Kernel & Template Migrations
- [ ] **Feature estimation completed**: Medium (M)
- [ ] **Feature acceptance criteria defined**

## Story/Enabler Level Issues
- [ ] **Technical Enabler: Notification Package Initialization** #TODO
  - Description: Create the `br.com.olympus.hermes.notification` base directories for `domain`, `application`, and `infrastructure`.
  - Labels: `enabler`, `infrastructure`, `priority-critical`
  - Estimate: 1
- [ ] **Technical Enabler: Relocate Notification Domain & Application** #TODO
  - Description: Move generic classes (`Notification`, multi-channel entities, CQRS handlers, projectors, ports) to the new package and clean up imports context-wide. 
  - Labels: `enabler`, `architecture`, `priority-critical`
  - Estimate: 3
- [ ] **Technical Enabler: Relocate Notification Infrastructure** #TODO
  - Description: Move DynamoDB EventStore implementations, Kafka consumers (`NotificationCreatedConsumer`, `DeliveryHandler`), external API adapters (Twilio, SES), and `NotificationController` to `notification/infrastructure`. 
  - Labels: `enabler`, `infrastructure`, `priority-critical`
  - Estimate: 5
- [ ] **Technical Enabler: Legacy Cleanup** #TODO
  - Description: Delete the now-empty root level legacy `core`, `infrastructure`, and `shared/domain` directories.
  - Labels: `enabler`, `infrastructure`, `priority-medium`
  - Estimate: 1
- [ ] **Test: End-to-End Regression Validation** #TODO
  - Description: Run full unit & integration tests (`./mvnw clean verify`). Ensure TestContainers for Kafka, MongoDB, and DynamoDB spin up cleanly and reflection correctly binds the `notification` package REST endpoints and listeners.
  - Labels: `test`, `regression`, `priority-critical`
  - Estimate: 1
