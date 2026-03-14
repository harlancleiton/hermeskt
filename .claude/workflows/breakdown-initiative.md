---
description: Generates all documentation for an epic (PRD → Arch → Features → Stories → Tests)
---

# /breakdown-initiative — Full Epic Breakdown Workflow

> Orchestrates **all six breakdown skills** in logical dependency order to produce
> a complete set of planning artifacts from a single high-level idea.

## Dependency Graph

```
breakdown-epic-pm ─→ breakdown-epic-arch ─┐
                                          │
                    ┌─────────────────────┘
                    │   (for EACH feature / enabler)
                    ▼
              breakdown-feature-prd ─→ breakdown-feature-implementation ─→ breakdown-test
                                                                              │
                    ┌─────────────────────────────────────────────────────────┘
                    │   (after ALL features are done)
                    ▼
              breakdown-plan
```

## Artifact Directory

All artifacts are saved under:

```
/docs/ways-of-work/plan/{e1-epic-name}/
├── epic.md                                # Step 1 output
├── arch.md                                # Step 2 output
├── {f1-feature-name}/
│   ├── prd.md                             # Step 3a output
│   ├── implementation-plan.md             # Step 3b output
│   └── test-strategy.md                   # Step 3c output
├── project-plan.md                        # Step 4 output (a)
└── issues-checklist.md                    # Step 4 output (b)
```

---

## Workflow Steps

### Pre-requisite — Collect input from the user

Ask the user for the following information (do **not** proceed until you have both):

| Input            | Description                                         | Example                                                  |
| ---------------- | --------------------------------------------------- | -------------------------------------------------------- |
| **Epic Name**    | A kebab-case identifier for the epic (e.g. e1-...)  | `e1-notification-delivery`                               |
| **Epic Idea**    | A high-level description (2-5 sentences) of the epic | _"Allow users to receive real-time push notifications…"_ |
| **Target Users** | _(optional)_ Who is this for?                       | _"End users and administrators"_                         |

Store these values for all subsequent steps.

---

### Step 1 — Epic PRD (`breakdown-epic-pm`)

// turbo

**Skill**: `breakdown-epic-pm`
**Input**: Epic Name + Epic Idea + Target Users
**Output**: `/docs/ways-of-work/plan/{e1-epic-name}/epic.md`

1. Read the full SKILL.md at `/breakdown-epic-pm/SKILL.md`.
2. Follow the skill instructions exactly, acting as an expert Product Manager.
3. Generate the Epic PRD with all required sections:
   - Epic Name, Goal (Problem / Solution / Impact)
   - User Personas, High-Level User Journeys
   - Business Requirements (Functional + Non-Functional)
   - Success Metrics, Out of Scope, Business Value
4. Save the output to `/docs/ways-of-work/plan/{e1-epic-name}/epic.md`.
5. **Wait for user review and approval** before proceeding. Use `notify_user` with `PathsToReview` pointing to the generated file.

---

### Step 2 — Epic Architecture (`breakdown-epic-arch`)

// turbo

**Skill**: `breakdown-epic-arch`
**Input**: The approved `epic.md` from Step 1
**Output**: `/docs/ways-of-work/plan/{e1-epic-name}/arch.md`

1. Read the full SKILL.md at `/breakdown-epic-arch/SKILL.md`.
2. Read the approved `epic.md` as the Epic PRD context.
3. Follow the skill instructions exactly, acting as a Senior Software Architect.
4. **Important:** Adapt the architecture context to the **Hermes project stack** (Kotlin + Quarkus, DDD, CQRS, Event Sourcing, Hexagonal Architecture, DynamoDB, MongoDB, Kafka, Arrow-kt) instead of the TypeScript/Next.js defaults mentioned in the skill.
5. Generate the Architecture Specification with all required sections:
   - Architecture Overview, System Architecture Diagram (Mermaid)
   - High-Level Features & Technical Enablers (this list drives Step 3)
   - Technology Stack, Technical Value, T-Shirt Size Estimate
6. Save the output to `/docs/ways-of-work/plan/{e1-epic-name}/arch.md`.
7. **Wait for user review and approval** before proceeding. Use `notify_user`.

> **Critical**: Extract the list of **features** and **technical enablers** from the arch.md.
> These become the iteration set for Step 3.

---

### Step 3 — Per-Feature Breakdown Loop

For **each feature / enabler** identified in `arch.md`, execute sub-steps 3a → 3b → 3c **in sequence**.
Features may be processed one at a time or in batches, but within each feature the order is strict.

#### Step 3a — Feature PRD (`breakdown-feature-prd`)

// turbo

**Skill**: `breakdown-feature-prd`
**Input**: Parent Epic (`epic.md` + `arch.md`) + Feature Idea (from arch.md feature list)
**Output**: `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/prd.md`

1. Read the full SKILL.md at `.agents/skills/breakdown-feature-prd/SKILL.md`.
2. Follow the skill instructions exactly, acting as an expert Product Manager.
3. Generate the Feature PRD with all required sections:
   - Feature Name, Epic reference, Goal
   - User Personas, User Stories (As a… I want… so that…)
   - Requirements (Functional + Non-Functional)
   - Acceptance Criteria (Given/When/Then or checklist)
   - Out of Scope
4. Save the output to `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/prd.md`.

#### Step 3b — Feature Implementation Plan (`breakdown-feature-implementation`)

// turbo

**Skill**: `breakdown-feature-implementation`
**Input**: The Feature PRD from Step 3a
**Output**: `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/implementation-plan.md`

1. Read the full SKILL.md at `.agents/skills/breakdown-feature-implementation/SKILL.md`.
2. Follow the skill instructions exactly, acting as a veteran software engineer.
3. **Important:** Adapt the implementation plan to the **Hermes project structure** (Hexagonal Architecture package layout, Kotlin conventions, DynamoDB write store, MongoDB read store, Kafka messaging) instead of the Epoch monorepo / TypeScript / shadcn defaults.
4. Generate the Implementation Plan with all required sections:
   - Goal, Requirements
   - Technical Considerations (System Architecture Diagram in Mermaid)
   - Database Schema Design (DynamoDB + MongoDB), API Design
   - Security & Performance considerations
5. Save the output to `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/implementation-plan.md`.

#### Step 3c — Feature Test Strategy (`breakdown-test`)

// turbo

**Skill**: `breakdown-test`
**Input**: Feature PRD (3a) + Implementation Plan (3b)
**Output**: `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/test-strategy.md`

1. Read the full SKILL.md at `/breakdown-test/SKILL.md`.
2. Follow the skill instructions exactly, acting as a senior QA Engineer.
3. Generate the Test Strategy with all required sections:
   - Test Strategy Overview (ISTQB framework)
   - ISO 25010 Quality Characteristics Assessment
   - Test Environment and Data Strategy
   - Test Issues Checklist, Quality Gates
4. Save the output to `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/test-strategy.md`.

#### Review Gate (per feature)

// turbo

After completing Steps 3a-3c for a feature:
- **Notify the user** with `PathsToReview` including `prd.md`, `implementation-plan.md`, and `test-strategy.md` for that feature.
- Wait for approval before proceeding to the next feature.

---

### Step 4 — Project Plan & Issues (`breakdown-plan`)

// turbo

**Skill**: `breakdown-plan`
**Input**: ALL approved artifacts from Steps 1-3 (epic.md, arch.md, and all feature artifacts)
**Output**:
  - `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/project-plan.md` (per feature)
  - `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/issues-checklist.md` (per feature)

1. Read the full SKILL.md at `/breakdown-plan/SKILL.md`.
2. Follow the skill instructions exactly, acting as a senior Project Manager.
3. Generate the Project Plan with all required sections:
   - Project Overview (summary, success criteria, milestones, risks)
   - Work Item Hierarchy (Epic → Feature → Story → Enabler → Test → Task)
   - GitHub Issues Breakdown (using all provided templates)
   - Priority & Value Matrix, Estimation, Dependency Management
   - Sprint Planning Template, GitHub Board Configuration
4. Generate the Issues Checklist (pre-creation preparation, epic/feature/story-level checklists).
5. Save the outputs.
6. **Notify the user** with all generated project plan files for final review.

---

## Completion Checklist

After all steps, verify the following files exist and are approved:

- [ ] `/docs/ways-of-work/plan/{e1-epic-name}/epic.md`
- [ ] `/docs/ways-of-work/plan/{e1-epic-name}/arch.md`
- [ ] For each feature:
  - [ ] `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/prd.md`
  - [ ] `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/implementation-plan.md`
  - [ ] `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/test-strategy.md`
  - [ ] `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/project-plan.md`
  - [ ] `/docs/ways-of-work/plan/{e1-epic-name}/{f1-feature-name}/issues-checklist.md`

## Error Handling

- If the user **rejects** an artifact at any review gate, re-execute ONLY that step with the user's feedback. Do NOT re-run downstream steps until the rejected artifact is approved.
- If the user requests **changes** after approval, identify which downstream artifacts are affected and re-generate them in dependency order.
- If the user wants to **skip** a feature, remove it from the iteration set and note it as "Out of Scope" in the project plan.