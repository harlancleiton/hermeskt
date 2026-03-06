# Issue Creation Checklist: Template Context Migration

## Pre-Creation Preparation
- [x] **Feature artifacts complete**: PRD, tech breakdown, test plan done
- [x] **Epic exists**: `refactor-modular-monolith` epic created
- [x] **Project board configured**: Kanban structure ready
- [x] **Team capacity assessed**: N/A for automated plan

## Feature Level Issues
- [ ] **Feature issue created** linking to parent epic
- [ ] **Feature dependencies identified**: Blocks Notification Context Migration
- [ ] **Feature estimation completed**: Small (S)
- [ ] **Feature acceptance criteria defined**

## Story/Enabler Level Issues
- [ ] **Technical Enabler: Template Package Initialization** #TODO
  - Description: Create the `br.com.olympus.hermes.template` base directories for `domain`, `application`, and `infrastructure`.
  - Labels: `enabler`, `infrastructure`, `priority-high`
  - Estimate: 1
- [ ] **Technical Enabler: Relocate Template Domain & Application** #TODO
  - Description: Move generic classes (`NotificationTemplate`, `TemplateEngine`, CQRS handlers) to the new package and clean up imports context-wide. 
  - Labels: `enabler`, `architecture`, `priority-high`
  - Estimate: 2
- [ ] **Technical Enabler: Relocate Template Infrastructure** #TODO
  - Description: Move MongoDB Panache entities/repos and REST controllers to `template/infrastructure`. 
  - Labels: `enabler`, `infrastructure`, `priority-high`
  - Estimate: 2
- [ ] **Test: Structural & Regression Validation** #TODO
  - Description: Run full unit & integration tests (`./mvnw clean verify`) to assure the package relocation didn't break Panache/CDI reflections. Ensure Sonar/Ktlint pass.
  - Labels: `test`, `regression`, `priority-high`
  - Estimate: 1
