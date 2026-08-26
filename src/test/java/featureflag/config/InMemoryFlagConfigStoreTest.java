package featureflag.config;

import featureflag.model.FlagConfig;
import featureflag.model.FlagValue;
import featureflag.model.FlagValueType;
import featureflag.model.Operator;
import featureflag.model.PercentageRollout;
import featureflag.model.TargetingRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryFlagConfigStoreTest {
    private final InMemoryFlagConfigStore store = new InMemoryFlagConfigStore();

    @Test
    void upsertsByNameAndEnvironment() {
        store.set(booleanFlag("checkout", "dev", false));
        store.set(booleanFlag("checkout", "dev", true));

        assertTrue(store.get("checkout", "dev").orElseThrow().defaultValue().asBoolean());
    }

    @Test
    void isolatesEnvironments() {
        store.set(booleanFlag("checkout", "dev", true));
        store.set(booleanFlag("checkout", "prod", false));

        assertTrue(store.get("checkout", "dev").orElseThrow().defaultValue().asBoolean());
        assertEquals(false, store.get("checkout", "prod").orElseThrow().defaultValue().asBoolean());
    }

    @Test
    void rejectsPercentageOutsideRangeAndLeavesStoreUnchanged() {
        store.set(booleanFlag("checkout", "prod", false));

        FlagConfig invalid = FlagConfig.builder()
                .name("checkout")
                .environment("prod")
                .valueType(FlagValueType.BOOLEAN)
                .defaultValue(FlagValue.ofBoolean(false))
                .rollout(new PercentageRollout(101, null, FlagValue.ofBoolean(true)))
                .build();

        assertThrows(InvalidFlagConfigException.class, () -> store.set(invalid));
        assertEquals(false, store.get("checkout", "prod").orElseThrow().defaultValue().asBoolean());
    }

    @Test
    void rejectsTypeMismatchOnRuleValue() {
        FlagConfig invalid = FlagConfig.builder()
                .name("checkout")
                .environment("prod")
                .valueType(FlagValueType.BOOLEAN)
                .defaultValue(FlagValue.ofBoolean(false))
                .rules(List.of(new TargetingRule(
                        "country",
                        Operator.EQUALS,
                        "IN",
                        FlagValue.ofString("yes"))))
                .build();

        assertThrows(InvalidFlagConfigException.class, () -> store.set(invalid));
        assertTrue(store.get("checkout", "prod").isEmpty());
    }

    @Test
    void notifiesListenersAfterSuccessfulSet() {
        AtomicReference<FlagConfig> seen = new AtomicReference<>();
        store.addListener(seen::set);
        FlagConfig config = booleanFlag("checkout", "prod", true);
        store.set(config);
        assertEquals("checkout", seen.get().name());
    }

    private static FlagConfig booleanFlag(String name, String env, boolean defaultValue) {
        return FlagConfig.builder()
                .name(name)
                .environment(env)
                .valueType(FlagValueType.BOOLEAN)
                .defaultValue(FlagValue.ofBoolean(defaultValue))
                .build();
    }
}
