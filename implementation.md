# Implementation

How the in-process feature flag library is organized, what each type does, and why it exists. Behavior still follows `PLAN.md`; this file is the class-level map.

## Package layout

Classes are split by responsibility so config I/O, pure evaluation, and the public SDK do not sit in one flat package.

```
featureflag.model        immutable domain (flags, rules, context, values)
featureflag.config       store, validation, live set/get
featureflag.evaluation   targeting + percentage bucketing
featureflag.client       embeddable API and error logging
```

```
Application
    FeatureFlagClient          (client)
        FlagConfigStore.get    (config)
        FlagEvaluator          (evaluation)
            RuleMatcher
            PercentageBucketer
    InMemoryFlagConfigStore.set
        FlagConfigValidator
```

Tests live in the same package as the unit they cover (`.../config`, `.../evaluation`, `.../client`).

---

## `featureflag.model`

Domain types only. No store, no hashing, no logging. Immutable so a `set` cannot tear a flag that another thread is evaluating.

| Class | Why it exists |
|-------|----------------|
| **`FlagValueType`** | Distinguishes boolean / string / integer so defaults, rule values, and getters stay aligned. |
| **`FlagValue`** | Holds one typed value. Avoids `Object` at API boundaries and makes type checks explicit. |
| **`Operator`** | v1 targeting operators (`EQUALS`, `NOT_EQUALS`, `IN`) without a strategy hierarchy. |
| **`EvaluationContext`** | Request-time identity and attributes (`userId`, `anonymousId`, `tenantId`, map). Builder for tests and call sites. `bucketIdentity()` centralizes sticky-rollout identity. |
| **`TargetingRule`** | One ordered rule: attribute, operator, operand, served value. First match wins in the evaluator. |
| **`PercentageRollout`** | Fallthrough rollout: `0–100`, optional `sharedBucketingKey`, `rolloutValue`. Independent vs shared salt lives here, not in the hasher. |
| **`FlagConfig`** | Full flag for one environment: name, env, type, default, rules, optional rollout. Builder for bootstrap/`set`. |

---

## `featureflag.config`

Configuration boundary from `PLAN.md`: `set` / `get`, validate at write time, in-memory source of truth.

| Class | Why it exists |
|-------|----------------|
| **`FlagConfigStore`** | Interface so the client depends on `set`/`get`/`addListener`, not a map. A later store can implement the same contract. |
| **`ConfigChangeListener`** | Optional push after a successful `set`. Client uses pull-on-read; listeners are for future/adapters and tests. |
| **`InvalidFlagConfigException`** | Config-path failure (may throw). Distinct from eval, which must not throw. |
| **`FlagConfigValidator`** | Package-private. Rejects bad percentage, type mismatch, blank name/env, empty `IN`. Keeps invalid configs out of the map. |
| **`InMemoryFlagConfigStore`** | Thread-safe upsert keyed by `(name, env)`. Validate → put → notify. `get` is the live-update path (next evaluate sees the write). Nested **`FlagKey`** is the map key. |

---

## `featureflag.evaluation`

Pure decision logic: given a `FlagConfig` and `EvaluationContext`, return a value and a reason. No I/O.

| Class | Why it exists |
|-------|----------------|
| **`RuleMatcher`** | Package-private. Walks rules in order; missing attribute = no match. Isolates operator comparison from rollout. |
| **`PercentageBucketer`** | Package-private. Deterministic SHA-256 → bucket `0–99`. Builds hash input (`flagName:identity` or `sharedKey:identity`) so stickiness and shared/independent schemes are testable. |
| **`FlagEvaluator`** | Orchestrates PLAN order: first matching rule, else rollout, else default. Missing identity → default + `MISSING_BUCKET_KEY`. Not `final` so fail-safe tests can force an exception. |
| **`EvaluationReason`** | Why a value was chosen (rule, in/out of %, default, missing bucket key). Used for logs and tests; not a public `evaluateDetails` API. |
| **`EvaluationResult`** | Pairs `FlagValue` with `EvaluationReason` so the client can log `MISSING_BUCKET_KEY` without re-deriving it. |

---

## `featureflag.client`

What the application embeds.

| Class | Why it exists |
|-------|----------------|
| **`FeatureFlagClient`** | Bound to one environment. Typed `getBoolean` / `getString` / `getInteger` with caller fallback. Pulls `store.get` each call (live updates, no snapshot). `try/catch` so callers never see exceptions. Extra constructor injects `FlagEvaluator` for error-path tests. |
| **`FlagErrorLogger`** | Structured errors (`FLAG_NOT_FOUND`, `TYPE_MISMATCH`, `EVAL_ERROR`, `INVALID_INPUT`, `MISSING_BUCKET_KEY`) without tying the library to a logging framework. |

---

## Test helpers (not production)

| Class | Why it exists |
|-------|----------------|
| **`RecordingFlagErrorLogger`** | Captures log codes so default-on-error tests can assert without a real logger. |

---

## What this split does not change

- Evaluation order, bucketing, fail-safe getters, and store semantics remain as in `PLAN.md`.
- No extra operators, segments, JSON loaders, or snapshot cache.
