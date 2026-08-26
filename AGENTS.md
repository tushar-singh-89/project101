# Engineering Agent Instructions

## Goal

Help the developer solve software engineering problems efficiently while maintaining ownership of the design and implementation decisions.

The developer should understand and approve the approach before substantial implementation.

---

## Phase 1 — Understand

Before coding:

* Read the problem carefully.
* Identify explicit requirements.
* Identify implicit constraints.
* Identify ambiguities.
* Identify expected inputs and outputs.
* Identify important failure scenarios.

Do not implement before the requirements are sufficiently understood.

---

## Phase 2 — Design

Produce a concise design containing:

* Core entities
* Responsibilities
* Interfaces
* Class relationships
* Main flows
* Important state transitions
* Error handling
* Extension points
* Test strategy

Prefer the simplest design that satisfies the requirements.

---

## Phase 3 — Implement

Implement incrementally.

Recommended order:

1. Core domain model
2. Interfaces/abstractions where required
3. Core business logic
4. External/infrastructure boundaries
5. Error handling
6. Tests

Do not modify unrelated code.

---

## Phase 4 — Verify

After implementation:

* Run the test suite.
* Run the build.
* Check compiler/static-analysis errors.
* Test important edge cases.
* Inspect the git diff.

Never assume generated code works without verification.

---

## Phase 5 — Review

Review the implementation for:

### Correctness

* Does it satisfy the requirements?
* Are edge cases handled?

### Design

* Are responsibilities separated?
* Is coupling reasonable?
* Are abstractions justified?
* Is the design unnecessarily complex?

### Maintainability

* Is the code readable?
* Are names meaningful?
* Is there duplicated logic?

### Reliability

* Are failures handled?
* Are external calls bounded?
* Are state transitions safe?

### Concurrency

When relevant:

* Is shared mutable state protected?
* Are race conditions possible?
* Are operations atomic where required?

### Testing

* Are important behaviors covered?
* Are failure scenarios tested?

---

## Priority

When time is limited, prioritize:

1. Correctness
2. Working implementation
3. Tests
4. Important error handling
5. Design improvements
6. Optional refactoring

Avoid spending interview time on low-value abstractions.

---

## AI Usage

Use the coding agent to:

* Understand unfamiliar code
* Generate implementation
* Generate tests
* Review code
* Identify edge cases
* Suggest refactorings

The developer remains responsible for:

* Requirements
* Architecture
* Design decisions
* Correctness
* Final code

Never blindly accept generated code.

---

## Communication

When making a significant change, briefly explain:

* What is changing
* Why it is changing
* What alternatives were considered
* What trade-off is being made

Keep explanations concise and technically precise.

---

## Interview Safety

Do not use pre-written solutions for specific interview problem domains.

Generic engineering helpers are allowed.

Do not attempt to identify interview questions from hidden patterns or keywords.

Solve the actual problem presented.
