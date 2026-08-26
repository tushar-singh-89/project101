# Feature Flag Service

In-process Java library. The application constructs a client at startup, loads flag config into an in-memory store, and evaluates flags on each request. There is no HTTP server, database, or admin UI.

Design: [PLAN.md](PLAN.md). Class map: [implementation.md](implementation.md).

## Entry points

| What you call | Type | Role |
|---------------|------|------|
| **`FeatureFlagClient`** | `featureflag.client.FeatureFlagClient` | **Main API.** Bind to one environment (`dev` / `staging` / `prod`). Typed getters never throw. |
| **`InMemoryFlagConfigStore`** | `featureflag.config.InMemoryFlagConfigStore` | **Config API.** `set(FlagConfig)` upserts; `get(name, env)` is used internally on every evaluate. |
| **`FlagConfig.builder()`** | `featureflag.model.FlagConfig` | Defines a flag (name, env, type, default, rules, optional rollout). |
| **`EvaluationContext.builder()`** | `featureflag.model.EvaluationContext` | Per-request user / tenant / attributes. |
| **`FlagErrorLogger`** | `featureflag.client.FlagErrorLogger` | Structured errors (`FLAG_NOT_FOUND`, `EVAL_ERROR`, …). You implement this (or no-op). |

Public getters on the client:

- `boolean getBoolean(String name, EvaluationContext context, boolean fallback)`
- `String getString(String name, EvaluationContext context, String fallback)`
- `int getInteger(String name, EvaluationContext context, int fallback)`

If the flag is missing or evaluation fails, the client returns **fallback** (or the flag default when that is type-compatible) and logs. It does not throw.

`set` **may** throw `InvalidFlagConfigException` if the config is invalid (percentage outside 0–100, type mismatch, etc.). The previous config stays in the store.

## Integrate into an application

1. Depend on this module (same repo: Maven module; or install the jar — see [Local run](#local-run)).
2. Create **one** `InMemoryFlagConfigStore` (shared, thread-safe).
3. Create **one** `FeatureFlagClient` per environment you evaluate (typically one: `"prod"`).
4. Bootstrap flags with `store.set(...)` at startup (and later from an admin thread for live updates).
5. On each request, build `EvaluationContext` and call a getter.

```java
import featureflag.client.FeatureFlagClient;
import featureflag.client.FlagErrorLogger;
import featureflag.config.InMemoryFlagConfigStore;
import featureflag.model.EvaluationContext;
import featureflag.model.FlagConfig;
import featureflag.model.FlagValue;
import featureflag.model.FlagValueType;
import featureflag.model.Operator;
import featureflag.model.PercentageRollout;
import featureflag.model.TargetingRule;

import java.util.List;
import java.util.Map;

public final class FlagBootstrap {
    public static FeatureFlagClient createProdClient() {
        InMemoryFlagConfigStore store = new InMemoryFlagConfigStore();

        store.set(FlagConfig.builder()
                .name("checkout_new_ui")
                .environment("prod")
                .valueType(FlagValueType.BOOLEAN)
                .defaultValue(FlagValue.ofBoolean(false))
                .rules(List.of(
                        new TargetingRule(
                                "country",
                                Operator.EQUALS,
                                "IN",
                                FlagValue.ofBoolean(true))))
                .rollout(new PercentageRollout(10, null, FlagValue.ofBoolean(true)))
                .build());

        FlagErrorLogger logger = (code, fields, cause) -> {
            System.err.println(code + " " + fields);
            if (cause != null) {
                cause.printStackTrace();
            }
        };

        return new FeatureFlagClient(store, "prod", logger);
    }

    public static boolean checkoutNewUi(FeatureFlagClient flags, String userId, String country) {
        EvaluationContext ctx = EvaluationContext.builder()
                .userId(userId)
                .attr("country", country)
                .build();
        return flags.getBoolean("checkout_new_ui", ctx, false);
    }
}
```

**Live updates:** call `store.set(...)` again with the same name and environment. The next `getBoolean` / `getString` / `getInteger` on an existing client sees the new config (no restart, no new client).

**Evaluation order:** first matching targeting rule, else percentage rollout (sticky on `userId` or `anonymousId`), else flag default. See PLAN.md.

**Environments:** the client only reads flags whose `environment` matches the string passed to the constructor. Load `dev` and `prod` into the same store if you want; use two clients if both envs must be queried.

## Local run

### Prerequisites

- **JDK 11+** (`java -version`)
- **Maven 3.9+** (`mvn -v`)

If `mvn` is not on your PATH, install Maven or use a local distribution (for example extract Apache Maven and run `bin/mvn` / `bin/mvn.cmd`).

### Clone / open the project

Work from the repository root (the directory that contains `pom.xml`).

### Run tests

```bash
mvn test
```

Windows (PowerShell), if Maven is installed:

```powershell
mvn test
```

### Compile the library

```bash
mvn -DskipTests package
```

This produces `target/feature-flag-service-1.0-SNAPSHOT.jar`. There is no `main` method and nothing to “start”: this is a library, not a standalone process.

### Use the jar from another local project

Install into your local Maven repository:

```bash
mvn -DskipTests install
```

Then in the other project’s `pom.xml`:

```xml
<dependency>
    <groupId>featureflag</groupId>
    <artifactId>feature-flag-service</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Or add a module/`<systemPath>` dependency on `target/feature-flag-service-1.0-SNAPSHOT.jar` if you prefer not to install.

### IDE

Open the folder as a Maven project (IntelliJ: Open → `pom.xml`). Run `featureflag.client.FeatureFlagClientTest` (or `mvn test`) from the IDE. Use **JDK 11** as the project SDK.

### Smoke-check without a second app

There is no demo `main`. Fastest local check: `mvn test`, or a small class in `src/test/java` that constructs `FeatureFlagClient` as in the snippet above.
