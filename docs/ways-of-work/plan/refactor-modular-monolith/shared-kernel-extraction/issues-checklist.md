# Issue Creation Checklist: Shared Kernel Extraction

## Pre-Creation Preparation
- [x] **Feature artifacts complete**: PRD, tech breakdown, test plan done
- [x] **Epic exists**: `refactor-modular-monolith` epic created
- [x] **Project board configured**: Kanban structure ready
- [x] **Team capacity assessed**: N/A for automated plan

## Epic Level Issues
- [ ] **Epic issue created** with comprehensive description and acceptance criteria
- [ ] **Epic milestone created** with target release date
- [ ] **Epic labels applied**: `epic`, `priority-critical`, `value-high`
- [ ] **Epic added to project board**

## Feature Level Issues
- [ ] **Feature issue created** linking to parent epic
- [ ] **Feature dependencies identified**: Blocks Template & Notification Contexts
- [ ] **Feature estimation completed**: Small (S)
- [ ] **Feature acceptance criteria defined**

## Story/Enabler Level Issues
- [ ] **Technical Enabler: Shared Package Structure Initialization** #TODO
  - Description: Create the `br.com.olympus.hermes.shared` base directories for `domain`, `application`, and `infrastructure`.
  - Labels: `enabler`, `infrastructure`, `priority-critical`
  - Estimate: 1
- [ ] **Technical Enabler: Relocate Generic Core Components** #TODO
  - Description: Move generic classes (`BaseEntity`, CQRS interfaces, `DomainExceptionMapper`, etc.) to the new package and clean up imports project-wide. 
  - Labels: `enabler`, `infrastructure`, `priority-critical`
  - Estimate: 3
- [ ] **Test: Structural & Regression Validation** #TODO
  - Description: Run full unit & integration tests (`./mvnw clean verify`) to assure the package relocation didn't break Panache/CDI reflections. Ensure Sonar/Ktlint pass.
  - Labels: `test`, `regression`, `priority-critical`
  - Estimate: 1
