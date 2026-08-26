# Feature Flag Service — Plan

In-process SDK that an application embeds at runtime, updates in memory, and evaluates synchronously. No persistent store, UI, network, or multi-region.

This document is the source of truth for implementation. Do not add operators, segments, JSON loaders, snapshot caches, or a public `evaluateDetails` API unless a later extension requires them.

---

## 1. Requirements

| # | Requirement | Implementation meaning |
|---|-------------|------------------------|
| 1 | Flag types | Boolean, string, integer. Each flag has a name, typed default, and value type. |
| 2 | Targeting rules | First-match rules on evaluation context attributes (e.g. `country == "IN"`). |
| 3 | Percentage rollouts | Sticky bucketing 0–99. Independent vs shared salt (see §2). |
| 4 | Environments | Config keyed by `(flag name, environment)`. Client bound to one env. |
| 5 | Live updates | Successful `set` is visible on the next evaluate. In-process, no restart. |
| 6 | Evaluation API | Synchronous; never throws to the caller. Errors → default/fallback + structured log. |
| 7 | Config source | In-memory store: `set(FlagConfig)`, `get(name, env)`. |
| 8 | Tests | Types, targeting, stickiness, env isolation, default-on-error. |

Out of scope: DB, UI, transport, replication.

---

## 2. Assumptions (locked)

### A. Bucketing key

- Identity: `userId` if present, else `anonymousId`. If neither is present, skip rollout, return default, log `MISSING_BUCKET_KEY`.
- **Independent (default):** hash input = `flagName + ":" + identity`.
- **Shared:** flag sets optional `sharedBucketingKey`. Hash input = `sharedBucketingKey + ":" + identity`. Same key + same identity → same bucket `0–99`. In-rollout iff `bucket < percentage`.
- Algorithm: SHA-256 of UTF-8 input, take first 8 bytes as unsigned long, `mod 100`.
- Tenant is **not** in the hash unless a future extension adds it.

### B. Targeting vs percentage

1. Load config for `(name, clientEnv)`.
2. First matching targeting rule wins → that rule’s served value.
3. Else if rollout present → bucket vs percentage → `rolloutValue` or default.
4. Else default.

Percentage is fallthrough, not AND with rules. Rules do not carry nested percentages in v1.

For non-boolean flags, rollout uses explicit `rolloutValue` (validated to match `valueType`).

### C. Misconfiguration

- **Config time (`set`):** reject percentage not in `0–100`; type mismatches; empty name/env; null required fields; empty `IN` list. Throw `InvalidFlagConfigException`. Store unchanged.
- **Eval time:** never throw. Missing flag → caller fallback. Internal error or type mismatch → flag default if type-compatible, else caller fallback. Always structured log.

### D. Config consumption

**Pull-on-read:** each evaluate calls `store.get(name, env)`. `ConcurrentHashMap` makes `set` immediately visible (under 5s). Optional `ConfigChangeListener` may notify on `set` for future use; the client does **not** maintain a snapshot cache.

### Other

- Java library, JUnit 5, no extra runtime dependencies.
- Client constructed for one environment.
- Public API: `getBoolean` / `getString` / `getInteger` with caller fallback.
- Operators v1: `EQUALS`, `NOT_EQUALS`, `IN`.
- Missing rule attribute → rule does not match (not an error).
- Inject `FlagErrorLogger` for tests.
- `FlagConfig` immutable after validation. Concurrent evaluate + `set` is required.
- `set` is upsert on `(name, env)`. No `delete` in v1.
- Null flag name or null context: catch, log, return fallback.

---

## 3. Core Entities

| Entity | Responsibility |
|--------|----------------|
| `FlagValueType` | BOOLEAN, STRING, INTEGER |
| `FlagValue` | Immutable typed value |
| `EvaluationContext` | userId, anonymousId, tenantId, attribute map; builder |
| `Operator` | EQUALS, NOT_EQUALS, IN |
| `TargetingRule` | attribute, operator, operand, servedValue |
| `PercentageRollout` | percentage 0–100, optional sharedBucketingKey, rolloutValue |
| `FlagConfig` | name, environment, type, default, rules, optional rollout |
| `InMemoryFlagConfigStore` | thread-safe set/get; notify listeners after successful set |
| `FlagEvaluator` | pure `(config, context) → result` |
| `PercentageBucketer` | identity + salt → bucket 0–99 |
| `FlagConfigValidator` | used by `set` |
| `FeatureFlagClient` | typed getters, env, fail-safe, logger |
| `FlagErrorLogger` | structured error sink |

---

## 4. Interfaces

```
FlagConfigStore
  void set(FlagConfig config)
  Optional<FlagConfig> get(String name, String environment)
  void addListener(ConfigChangeListener listener)

ConfigChangeListener
  void onConfigChanged(FlagConfig config)

FlagErrorLogger
  void error(String code, Map<String, String> fields, Throwable cause)
```

No interfaces for evaluator, bucketer, or validator. No Decorator class around the client; `try/catch` lives in the three getters.

---

## 5. Relationships

```
Application
  → FeatureFlagClient(store, env, logger)
       → FlagConfigStore.get on each evaluate
       → FlagEvaluator (rules then percentage)
            → PercentageBucketer
  → FlagConfigStore.set (bootstrap / live update)
       → FlagConfigValidator
       → ConcurrentHashMap upsert
       → ConfigChangeListener (optional)
```

---

## 6. Main Flows

### Embed

1. Create `InMemoryFlagConfigStore`.
2. Create `FeatureFlagClient(store, "prod", logger)`.
3. Bootstrap via `store.set(...)`.
4. Request path calls typed getters with `EvaluationContext`.

### set

Validate → upsert → notify listeners. Next `get` sees new config.

### getBoolean / getString / getInteger

Lookup → evaluate in try/catch → unbox. Never throw.

### Stickiness

Same identity + same hash input → same bucket. Shared key aligns buckets across flags.

---

## 7. Edge Cases

- Invalid `set`: exception; previous config still used.
- Empty store / unknown flag: fallback + `FLAG_NOT_FOUND`.
- Percentage 0 and 100; buckets 0 and 99.
- First matching rule wins.
- Concurrent set during evaluate: immutable configs, atomic map replace; no torn flag.
- Missing bucket identity: default + `MISSING_BUCKET_KEY`.
- Changing `sharedBucketingKey` re-buckets (documented, not a bug).

---

## 8. Design Patterns

- Immutable configs + `ConcurrentHashMap` (no extra snapshot).
- Pull-on-read for live updates.
- Operator `switch` in matcher (not Strategy objects).
- Fail-safe in getters (not a Decorator type).

---

## 9. Test Strategy

- Types: default returned for bool/string/int with no rules.
- Targeting: match, miss, first-wins, missing attribute, IN / NOT_EQUALS.
- Percentage: same user sticky; 0 and 100; independent flags can diverge; shared key aligns; missing userId does not throw.
- Precedence: matching rule overrides rollout.
- Environments: same name, different env, isolated.
- Default-on-error: unknown flag; forced eval failure; type mismatch getter; invalid set rejected.
- Live update: evaluate, `set`, evaluate again without new client.

---

## 10. Implementation Order

1. Maven + JUnit skeleton
2. Domain types (immutable)
3. Validator + in-memory store
4. Rule matcher + evaluator without percentage
5. Bucketer + rollout + precedence
6. Client fail-safe getters + logger
7. Env isolation + live update tests

Stop. No extra features beyond this plan.
