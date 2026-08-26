# Code Review

Review the current implementation as a senior software engineer.

Do not modify files.

Review in this order:

## 1. Correctness

Check whether the implementation actually satisfies the requirements.

Identify concrete bugs.

## 2. Edge Cases

Look for:

* Invalid input
* Null/empty values
* Boundary conditions
* Duplicate operations
* Invalid state transitions
* External failures

## 3. Design

Check:

* SOLID principles
* Class responsibilities
* Coupling
* Cohesion
* Abstraction quality
* Inheritance vs composition
* Unnecessary design patterns
* Unnecessary complexity

## 4. Reliability

Check:

* Exception handling
* External calls
* Timeouts
* Retry behavior
* Idempotency where relevant
* Failure recovery

## 5. Concurrency

If relevant, check:

* Shared mutable state
* Race conditions
* Thread safety
* Atomic operations
* Concurrent collections
* Locking

## 6. Testing

Check whether the tests cover:

* Happy path
* Edge cases
* Failure scenarios
* Important state transitions

## 7. Code Quality

Check:

* Naming
* Duplication
* Readability
* Method/class size
* Unnecessary comments
* Dead code

## Output

Categorize findings:

P0 — Correctness bug / blocking issue

P1 — Important design or reliability issue

P2 — Improvement / cleanup

For every issue provide:

* Problem
* Why it matters
* Suggested fix

Do not suggest changes merely for stylistic preference.
