---
description: Generates all documentation for an epic (PRD → Arch → Features → Stories → Tests)
auto_execution_mode: 2
---

1. Receive from the user: epic name + high-level idea
2. Execute breakdown-epic-pm → generates epic.md
3. Execute breakdown-epic-arch with epic.md → generates arch.md
4. For each feature identified in arch.md:
   a. Execute breakdown-feature-prd → generates prd.md
   b. Execute breakdown-feature-implementation → generates implementation-plan.md
   c. Execute breakdown-test → generates test-strategy.md
5. Execute breakdown-plan for all features → generates project-plan.md
