---
name: code-review
description: >
  This skill should be used when the user asks to "review my code", "check my
  changes", "review this file", "look for bugs in my code", "audit my code",
  "check for security issues", "review for SOLID principles", "review for clean
  code", "find issues in my code", "code review before I commit", or any similar
  local code review request. Also use when the user says "is this code ok?",
  "does this look right?", "check my implementation", or "review what I just
  wrote". Do NOT use when the user asks to review a GitHub pull request, post a
  PR comment, or use gh CLI — use the /code-review command instead.
version: 0.1.0
tools: Read, Glob, Grep, Bash, Edit
---

# Local Code Review

Perform a thorough local code review across five dimensions: SOLID & clean code
principles, bugs & logic errors, security vulnerabilities, code quality &
conventions, and performance. Produce an issues list and apply safe auto-fixes.

## Step 1 — Determine What to Review

Detect the review scope in this order:

1. If the user named specific files or directories, use those.
2. Otherwise, run `git diff --name-only HEAD` (staged + unstaged). If that
   returns files, use them. If the repo has no commits yet, run
   `git diff --cached --name-only`.
3. If no git diff is available or the diff is empty, ask the user which files
   to review. Do not proceed silently on an empty scope.

After resolving the file list, read the full content of each file.

## Step 2 — Load Project Conventions

Look for `CLAUDE.md` files and read them:

```bash
find . -name "CLAUDE.md" 2>/dev/null | head -20
```

Read the root `CLAUDE.md` first, then any `CLAUDE.md` files in directories
that contain the files under review. Record any explicit style rules, forbidden
patterns, required patterns, and naming conventions. These become the benchmark
for the "code quality & conventions" dimension.

If no `CLAUDE.md` exists, note that and apply general clean-code standards only.

## Step 3 — Parallel Analysis (Five Subagents)

Launch five subagents in parallel, one per dimension. Each subagent receives:
- The full content of every file under review
- The CLAUDE.md conventions (or a note that none exist)
- The rubric for its dimension (see `references/rubrics.md`)

Each subagent must return a list of findings. Every finding must include:

| Field | Description |
|-------|-------------|
| `dimension` | One of: solid, bugs, security, quality, performance |
| `severity` | One of: critical, high, medium, low |
| `file` | Relative file path |
| `line_range` | Start–end line numbers, e.g., `42-47` |
| `description` | What the issue is and why it matters |
| `suggested_fix` | Concrete code change or guidance |
| `confidence` | Integer 0–100 |
| `auto_fixable` | Boolean — safe to apply without ambiguity |

Dimension assignments:

- **Subagent A — SOLID & Clean Code**: Single responsibility, open/closed,
  Liskov substitution, interface segregation, dependency inversion. Also:
  excessive function length, deep nesting, magic numbers, poor naming,
  duplicated logic. Read `references/rubrics.md#solid` before starting.

- **Subagent B — Bugs & Logic Errors**: Null/undefined dereferences,
  off-by-one errors, incorrect boolean logic, unhandled promise rejections,
  race conditions, missing return values, incorrect type coercions, dead code
  that masks real failures. Read `references/rubrics.md#bugs`.

- **Subagent C — Security Vulnerabilities**: OWASP Top 10, SQL/command/HTML
  injection, hardcoded credentials or secrets, missing input validation,
  insecure deserialization, broken authentication patterns, excessive data
  exposure in logs or responses, missing authorization checks, path traversal.
  Read `references/rubrics.md#security`.

- **Subagent D — Code Quality & Conventions**: Cross-reference every finding
  against CLAUDE.md. Flag only explicit violations, not generic style opinions.
  Also check: inconsistent error handling, missing or incorrect test coverage
  for new public APIs, poor abstraction boundaries. Read
  `references/rubrics.md#quality`.

- **Subagent E — Performance**: O(n²) or worse algorithms where O(n log n) is
  feasible, N+1 query patterns, unnecessary re-computation inside loops,
  unbounded memory accumulation, synchronous I/O blocking the event loop,
  redundant network calls. Read `references/rubrics.md#performance`.

## Step 4 — Confidence Filter

Collect all findings from all five subagents. Discard any finding with
`confidence < 80`. The confidence rubric each subagent must use:

| Score | Meaning |
|-------|---------|
| 0–25  | Likely false positive; pre-existing issue; cannot verify |
| 26–50 | Possible issue but not verifiable; nitpick not in CLAUDE.md |
| 51–75 | Valid but low-impact; might not trigger in practice |
| 76–89 | Important; verified real issue; will affect functionality |
| 90–100 | Certain; confirmed defect or explicit CLAUDE.md violation |

Also discard findings that are:
- Pre-existing in lines the current diff did not touch (when reviewing a diff)
- Issues a linter, type checker, or compiler would catch in CI
- Style issues not explicitly mentioned in CLAUDE.md
- Intentional overrides (e.g., a lint-ignore comment is present and justified)

## Step 5 — Auto-Fix Pass

For every remaining finding where `auto_fixable` is `true` AND `confidence >= 80`:

Determine whether the fix is unambiguous — there is exactly one correct
mechanical transformation and no architectural judgment is required. Apply the
fix using the Edit tool only if ALL of the following hold:

- The change is self-contained to one file
- No interface or public API contract changes
- Not a design/architecture decision
- Reversible with a one-line diff

Record every applied fix with: file, line_range, brief description of what changed.

Do not auto-fix:
- Architecture or design pattern changes
- Refactors that affect multiple call sites
- Security fixes that require understanding the broader auth flow
- Any finding below confidence 80

## Step 6 — Output Report

Always produce output in this exact structure:

---

### Auto-Fixed Issues

List each fix applied. If nothing was auto-fixed, write "None."

Format per fix:
```
- [severity] file.ts:42-47 — Brief description of what was fixed
```

---

### Issues Requiring Your Attention

Group by severity (critical first). For each issue:

```
#### [CRITICAL|HIGH|MEDIUM|LOW] — dimension — file.ts:42-47

**What:** Concrete description of the problem.
**Why it matters:** Impact on correctness, security, or maintainability.
**Suggested fix:**
<code snippet or clear prose instruction>
```

If no issues remain after filtering, write:

> No issues found above the confidence threshold. The reviewed code appears
> clean across all five dimensions.

---

### Review Summary

End with a one-paragraph summary: how many files reviewed, how many issues
found per severity, how many were auto-fixed, any notable patterns across
dimensions.

---

## Scope Boundaries

This skill covers local development review only. It does not:
- Post comments to GitHub pull requests (use `/code-review` command instead)
- Run the test suite or build system
- Review files outside the determined scope
- Attempt to fix architecture issues

## Additional Resources

For the detailed per-dimension rubrics used by each subagent, see:
- `references/rubrics.md` — Full scoring rubrics for all five dimensions
