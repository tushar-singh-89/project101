package featureflag.client;

import featureflag.config.ConfigChangeListener;
import featureflag.config.FlagConfigStore;
import featureflag.config.InMemoryFlagConfigStore;
import featureflag.evaluation.EvaluationResult;
import featureflag.evaluation.FlagEvaluator;
import featureflag.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureFlagClientTest {

    @Test
    void evaluatesBooleanStringAndInteger() {
        InMemoryFlagConfigStore store = new InMemoryFlagConfigStore();
        store.set(flag("bool", "prod", FlagValueType.BOOLEAN, FlagValue.ofBoolean(true), null));
        store.set(flag("str", "prod", FlagValueType.STRING, FlagValue.ofString("on"), null));
        store.set(flag("num", "prod", FlagValueType.INTEGER, FlagValue.ofInteger(3), null));
        RecordingFlagErrorLogger logger = new RecordingFlagErrorLogger();
        FeatureFlagClient client = new FeatureFlagClient(store, "prod", logger);
        EvaluationContext ctx = EvaluationContext.builder().userId("u1").build();

        assertTrue(client.getBoolean("bool", ctx, false));
        assertEquals("on", client.getString("str", ctx, "off"));
        assertEquals(3, client.getInteger("num", ctx, 0));
        assertTrue(logger.codes().isEmpty());
    }

    @Test
    void isolatesEnvironmentsOnTheClient() {
        InMemoryFlagConfigStore store = new InMemoryFlagConfigStore();
        store.set(flag("checkout", "dev", FlagValueType.BOOLEAN, FlagValue.ofBoolean(true), null));
        store.set(flag("checkout", "prod", FlagValueType.BOOLEAN, FlagValue.ofBoolean(false), null));
        FeatureFlagClient prod = new FeatureFlagClient(store, "prod", new RecordingFlagErrorLogger());
        FeatureFlagClient dev = new FeatureFlagClient(store, "dev", new RecordingFlagErrorLogger());
        EvaluationContext ctx = EvaluationContext.builder().userId("u1").build();

        assertFalse(prod.getBoolean("checkout", ctx, true));
        assertTrue(dev.getBoolean("checkout", ctx, false));
    }

    @Test
    void liveSetIsVisibleWithoutNewClient() {
        InMemoryFlagConfigStore store = new InMemoryFlagConfigStore();
        store.set(flag("checkout", "prod", FlagValueType.BOOLEAN, FlagValue.ofBoolean(false), null));
        FeatureFlagClient client = new FeatureFlagClient(store, "prod", new RecordingFlagErrorLogger());
        EvaluationContext ctx = EvaluationContext.builder().userId("u1").build();

        assertFalse(client.getBoolean("checkout", ctx, true));
        store.set(flag("checkout", "prod", FlagValueType.BOOLEAN, FlagValue.ofBoolean(true), null));
        assertTrue(client.getBoolean("checkout", ctx, false));
    }

    @Test
    void unknownFlagReturnsCallerFallbackAndLogs() {
        RecordingFlagErrorLogger logger = new RecordingFlagErrorLogger();
        FeatureFlagClient client = new FeatureFlagClient(new InMemoryFlagConfigStore(), "prod", logger);

        boolean value = client.getBoolean("missing", EvaluationContext.builder().userId("u1").build(), true);

        assertTrue(value);
        assertTrue(logger.hasCode("FLAG_NOT_FOUND"));
    }

    @Test
    void typeMismatchReturnsFallbackAndLogs() {
        InMemoryFlagConfigStore store = new InMemoryFlagConfigStore();
        store.set(flag("theme", "prod", FlagValueType.STRING, FlagValue.ofString("dark"), null));
        RecordingFlagErrorLogger logger = new RecordingFlagErrorLogger();
        FeatureFlagClient client = new FeatureFlagClient(store, "prod", logger);

        assertFalse(client.getBoolean("theme", EvaluationContext.builder().userId("u1").build(), false));
        assertTrue(logger.hasCode("TYPE_MISMATCH"));
    }

    @Test
    void storeFailureReturnsFlagDefaultAndDoesNotThrow() {
        FlagConfig config = flag("checkout", "prod", FlagValueType.BOOLEAN, FlagValue.ofBoolean(true), null);
        FlagConfigStore store = new ThrowingAfterGetStore(config);
        RecordingFlagErrorLogger logger = new RecordingFlagErrorLogger();
        FeatureFlagClient client = new FeatureFlagClient(store, "prod", logger, new ThrowingEvaluator());

        boolean value = assertDoesNotThrow(
                () -> client.getBoolean("checkout", EvaluationContext.builder().userId("u1").build(), false));

        assertTrue(value);
        assertTrue(logger.hasCode("EVAL_ERROR"));
    }

    @Test
    void missingBucketKeyLogsAndReturnsDefault() {
        InMemoryFlagConfigStore store = new InMemoryFlagConfigStore();
        store.set(flag(
                "checkout",
                "prod",
                FlagValueType.BOOLEAN,
                FlagValue.ofBoolean(false),
                new PercentageRollout(100, null, FlagValue.ofBoolean(true))));
        RecordingFlagErrorLogger logger = new RecordingFlagErrorLogger();
        FeatureFlagClient client = new FeatureFlagClient(store, "prod", logger);

        assertFalse(client.getBoolean("checkout", EvaluationContext.builder().attr("country", "IN").build(), true));
        assertTrue(logger.hasCode("MISSING_BUCKET_KEY"));
    }

    @Test
    void nullInputsReturnFallbackAndDoNotThrow() {
        RecordingFlagErrorLogger logger = new RecordingFlagErrorLogger();
        FeatureFlagClient client = new FeatureFlagClient(new InMemoryFlagConfigStore(), "prod", logger);

        assertTrue(assertDoesNotThrow(() -> client.getBoolean(null, null, true)));
        assertTrue(logger.hasCode("INVALID_INPUT"));
    }

    private static FlagConfig flag(
            String name,
            String env,
            FlagValueType type,
            FlagValue defaultValue,
            PercentageRollout rollout) {
        return FlagConfig.builder()
                .name(name)
                .environment(env)
                .valueType(type)
                .defaultValue(defaultValue)
                .rules(List.of())
                .rollout(rollout)
                .build();
    }

    private static final class ThrowingEvaluator extends FlagEvaluator {
        @Override
        public EvaluationResult evaluate(FlagConfig config, EvaluationContext context) {
            throw new IllegalStateException("boom");
        }
    }

    private static final class ThrowingAfterGetStore implements FlagConfigStore {
        private final FlagConfig config;

        private ThrowingAfterGetStore(FlagConfig config) {
            this.config = config;
        }

        @Override
        public void set(FlagConfig config) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<FlagConfig> get(String name, String environment) {
            return Optional.of(config);
        }

        @Override
        public void addListener(ConfigChangeListener listener) {
            throw new UnsupportedOperationException();
        }
    }


    @Test
    void evaluatesCountrySpecificRolloutsForStringFlag() {
        InMemoryFlagConfigStore store = new InMemoryFlagConfigStore();

        store.set(FlagConfig.builder()
                .name("new-checkout")
                .environment("prod")
                .valueType(FlagValueType.STRING)
                .defaultValue(FlagValue.ofString("disabled"))
                .rules(List.of(
                        new TargetingRule(
                                "country",
                                Operator.EQUALS,
                                "IN",
                                FlagValue.ofString("enabled"))
                ))
                .rollout(new PercentageRollout(
                        50,
                        null,
                        FlagValue.ofString("enabled")))
                .build());

        FeatureFlagClient client =
                new FeatureFlagClient(
                        store,
                        "prod",
                        new RecordingFlagErrorLogger());

        EvaluationContext indiaUser = EvaluationContext.builder()
                .userId("user-india")
                .attr("country", "IN")
                .build();

        EvaluationContext usUser = EvaluationContext.builder()
                .userId("user-us")
                .attr("country", "US")
                .build();

        EvaluationContext australiaUser = EvaluationContext.builder()
                .userId("user-australia")
                .attr("country", "AU")
                .build();

        assertEquals(
                "enabled",
                client.getString("new-checkout", indiaUser, "disabled"));

        assertEquals(
                "enabled",
                client.getString("new-checkout", usUser, "disabled"));

        assertEquals(
                "enabled",
                client.getString("new-checkout", australiaUser, "disabled"));
    }
}
