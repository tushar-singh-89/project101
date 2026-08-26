package featureflag;

import featureflag.client.FeatureFlagClient;
import featureflag.config.InMemoryFlagConfigStore;
import featureflag.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureFlagServiceHappyPathTest {

    @Test
    void checkoutFeatureFlagHappyPath() {
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
                .rollout(new PercentageRollout(
                        10,
                        null,
                        FlagValue.ofBoolean(true)))
                .build());

        FeatureFlagClient client =
                new FeatureFlagClient(
                        store,
                        "prod",
                        (code, fields, cause) -> {});

        EvaluationContext context = EvaluationContext.builder()
                .userId("user-123")
                .attr("country", "IN")
                .build();

        boolean enabled =
                client.getBoolean(
                        "checkout_new_ui",
                        context,
                        false);

        assertTrue(enabled);
    }
}
