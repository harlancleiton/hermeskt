# Code Review Rubrics

Detailed scoring guidance for each review dimension. Each subagent should read
its own section before beginning analysis.

---

## SOLID & Clean Code {#solid}

### What to look for

**Single Responsibility Principle**
A class, module, or function should have one reason to change. Flag when:
- A function does both data fetching and rendering
- A class manages both business logic and persistence
- A module exports unrelated concerns under one namespace
- Functions exceed ~30 lines without a clear single purpose

**Open/Closed Principle**
Code should be open for extension, closed for modification. Flag when:
- A switch/if-else on a type tag would need modification to support new variants
- Adding a new feature requires touching an existing stable abstraction

**Liskov Substitution Principle**
Subtypes must be substitutable for their base types. Flag when:
- An override throws NotImplemented or narrows the contract
- A subclass changes preconditions or postconditions of an inherited method

**Interface Segregation Principle**
No client should depend on methods it does not use. Flag when:
- An interface has more than ~5-7 methods, some of which are irrelevant to many implementors
- A class implements an interface but leaves several methods as stubs or throws

**Dependency Inversion Principle**
Depend on abstractions, not concretions. Flag when:
- A high-level module directly imports and instantiates a low-level module
- A function hardcodes a concrete dependency rather than accepting it as a parameter

### Clean code checklist

- Magic numbers without named constants
- Boolean parameters (prefer enum or named object)
- Deeply nested code (>3 levels of indentation as a rough guide)
- Functions with >3 parameters not grouped into a config object
- Duplicated logic that could be extracted (DRY, but only when the duplication is clearly not incidental)
- Poor variable names (single letters outside loops, misleading names, Hungarian notation)
- Dead code (unreachable branches, unused imports, commented-out blocks left in)

### Confidence guidance

Score 90+ only for clear, textbook violations. Score 76-89 for probable
violations where context partially justifies the current approach. Score below
50 for matters of taste not codified in CLAUDE.md.

---

## Bugs & Logic Errors {#bugs}

### What to look for

**Null and undefined handling**
- Property access on a value that can be null/undefined without a guard
- Optional chaining used to silently skip an operation that should always succeed
- A function that returns undefined in some branch when callers expect a value

**Control flow errors**
- Off-by-one errors in loop bounds or array indexing
- Wrong operator precedence without parentheses (`a || b && c`)
- Negated condition that inverts intended logic
- Missing `break` in a switch with fallthrough intent not documented
- Unreachable `else` branch after a `return`

**Async / concurrency**
- Promise not awaited (fire-and-forget that should be awaited)
- Missing error handler on a Promise chain
- Race condition: two async operations both read then write shared state
- `Promise.all` where one rejection silently swallows other results

**Type coercions**
- `==` instead of `===` where the types may differ
- Numeric comparison with a string input
- `parseInt`/`parseFloat` without a radix or NaN check

**Missing cases**
- Switch or if-else that does not handle all variants of a discriminated union
- A callback that can fire multiple times when caller expects once

### Confidence guidance

Score 90+ only when the bug is demonstrably reproducible from the code alone.
Score 76-89 when reproducing it requires a specific input scenario you can
describe clearly. Do not flag issues where the surrounding code (outside the
diff) clearly handles the case.

---

## Security Vulnerabilities {#security}

### OWASP Top 10 checklist (abridged for code review)

**1. Injection** — SQL, OS command, LDAP, template injection
- User input interpolated directly into a query string or shell command
- Template literal with unsanitized user data passed to `eval`, `exec`,
  `querySelector`, or a query builder

**2. Broken Authentication**
- Hardcoded credentials or API keys in source files
- Tokens stored in localStorage without expiry or rotation
- Missing rate limiting on authentication endpoints (visible in code)

**3. Sensitive Data Exposure**
- PII, passwords, or secrets logged at any level
- Sensitive fields included in API responses without explicit allowlist
- Unencrypted sensitive data written to disk or sent over HTTP

**4. XML/JSON Injection & Deserialization**
- `JSON.parse` or XML parsing on untrusted input without schema validation
- Object deserialization that can instantiate arbitrary types

**5. Broken Access Control**
- Authorization check missing before a privileged operation
- Direct object reference without ownership check (e.g., `GET /users/:id`
  without verifying the requester owns that id)

**6. Security Misconfiguration**
- CORS set to `*` for sensitive endpoints
- Debug mode or verbose error messages enabled in code that ships to prod

**7. Cross-Site Scripting (XSS)**
- `innerHTML`, `dangerouslySetInnerHTML`, or `document.write` with user data
- Missing Content-Security-Policy headers set in application code

**8. Path Traversal**
- File path constructed from user input without normalization and boundary check

### Confidence guidance

Score 90+ for clear, exploitable vulnerabilities (e.g., a literal string
interpolation into a SQL query). Score 76-89 for probable vulnerabilities that
depend on how the function is called. Score below 50 for theoretical risks that
require multiple layers of defense to fail simultaneously.

---

## Code Quality & Conventions {#quality}

### CLAUDE.md cross-reference process

1. Read every rule in CLAUDE.md.
2. For each rule, check whether any code in the review scope violates it.
3. Flag only explicit violations — rules that are clearly stated and clearly
   broken. Do not invent rules not present in CLAUDE.md.
4. For rules that use language like "avoid" or "prefer", apply judgment: flag
   only when the violation is pronounced, not when the alternative was
   reasonable.

### General quality checks (apply when no CLAUDE.md rule covers the area)

**Error handling**
- Empty catch blocks that swallow errors silently
- Catch blocks that log and continue when the operation is not genuinely recoverable
- Broad `catch(e)` that could hide unrelated errors

**Abstraction quality**
- Functions that do too much (see SOLID above)
- Abstractions that leak their internals (exposing implementation details in the public interface)
- Missing abstraction: copy-pasted code that could be a shared utility

**Test coverage (for new public APIs)**
- A new exported function with no corresponding test file or test case
- A change to existing behavior with no update to the test that covers it

### Confidence guidance

Score 90+ only for explicit CLAUDE.md violations. Score 76-89 for clear general
quality issues. Score below 50 for anything that is a matter of team preference
not documented in CLAUDE.md.

---

## Performance {#performance}

### What to look for

**Algorithmic complexity**
- A nested loop over the same collection (O(n²)) where a map/set lookup (O(n)) is straightforward
- Sorting inside a loop
- Recursive function without memoization that recomputes the same subproblem

**I/O and network**
- N+1 query pattern: a database/API call inside a loop that could be batched
- Synchronous file or network I/O on a thread/event loop that serves requests
- Unbounded `SELECT *` where only a few columns are used

**Memory**
- Accumulating results into an array inside a loop when streaming would suffice
- Event listeners added without cleanup (memory leak pattern)
- Large objects cached indefinitely with no eviction

**Unnecessary computation**
- Recomputing an expensive value on every iteration when it is loop-invariant
- Serializing/deserializing the same data multiple times in sequence
- Repeated DOM queries inside a render loop

### Confidence guidance

Score 90+ only when the performance issue is demonstrably present at the code
level (e.g., a visible N+1 loop). Score 76-89 when the issue is likely but
depends on data volume assumptions you can state explicitly. Do not flag
micro-optimizations that would not be measurable in practice.
