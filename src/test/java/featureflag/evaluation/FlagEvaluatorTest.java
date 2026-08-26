package featureflag.evaluation;

import featureflag.model.EvaluationContext;
import featureflag.model.FlagConfig;
import featureflag.model.FlagValue;
import featureflag.model.FlagValueType;
import featureflag.model.Operator;
import featureflag.model.PercentageRollout;
import featureflag.model.TargetingRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlagEvaluatorTest {
    private final FlagEvaluator evaluator = new FlagEvaluator();
    private final PercentageBucketer bucketer = new PercentageBucketer();

    @Test
    void returnsTypedDefaultsWhenNoRulesOrRollout() {
        assertEquals(true, evaluator.evaluate(booleanFlag("f", true, List.of(), null), context("u1")).value().asBoolean());
        assertEquals("control", evaluator.evaluate(stringFlag("control"), context("u1")).value().asString());
        assertEquals(7, evaluator.evaluate(intFlag(7), context("u1")).value().asInteger());
    }

    @Test
    void matchingRuleReturnsServedValue() {
        TargetingRule india = new TargetingRule("country", Operator.EQUALS, "IN", FlagValue.ofBoolean(true));
        FlagConfig config = booleanFlag("checkout", false, List.of(india), null);

        EvaluationResult result = evaluator.evaluate(config, EvaluationContext.builder().userId("u1").attr("country", "IN").build());

        assertEquals(true, result.value().asBoolean());
        assertEquals(EvaluationReason.RULE_MATCH, result.reason());
    }

    @Test
    void nonMatchingRuleReturnsDefault() {
        TargetingRule india = new TargetingRule("country", Operator.EQUALS, "IN", FlagValue.ofBoolean(true));
        FlagConfig config = booleanFlag("checkout", false, List.of(india), null);

        EvaluationResult result = evaluator.evaluate(config, EvaluationContext.builder().userId("u1").attr("country", "US").build());

        assertEquals(false, result.value().asBoolean());
        assertEquals(EvaluationReason.DEFAULT, result.reason());
    }

    @Test
    void firstMatchingRuleWins() {
        TargetingRule first = new TargetingRule("country", Operator.EQUALS, "IN", FlagValue.ofBoolean(true));
        TargetingRule second = new TargetingRule("country", Operator.EQUALS, "IN", FlagValue.ofBoolean(false));
        FlagConfig config = booleanFlag("checkout", false, List.of(first, second), null);

        EvaluationResult result = evaluator.evaluate(config, EvaluationContext.builder().attr("country", "IN").build());

        assertEquals(true, result.value().asBoolean());
    }

    @Test
    void missingAttributeDoesNotMatch() {
        TargetingRule india = new TargetingRule("country", Operator.EQUALS, "IN", FlagValue.ofBoolean(true));
        FlagConfig config = booleanFlag("checkout", false, List.of(india), null);

        assertEquals(false, evaluator.evaluate(config, context("u1")).value().asBoolean());
    }

    @Test
    void inAndNotEqualsOperators() {
        TargetingRule inRule = new TargetingRule(
                "country",
                Operator.IN,
                List.of("IN", "SG"),
                FlagValue.ofBoolean(true));
        FlagConfig inConfig = booleanFlag("checkout", false, List.of(inRule), null);
        assertEquals(
                true,
                evaluator.evaluate(inConfig, EvaluationContext.builder().attr("country", "SG").build()).value().asBoolean());

        TargetingRule notUs = new TargetingRule("country", Operator.NOT_EQUALS, "US", FlagValue.ofBoolean(true));
        FlagConfig notConfig = booleanFlag("checkout", false, List.of(notUs), null);
        assertEquals(
                true,
                evaluator.evaluate(notConfig, EvaluationContext.builder().attr("country", "IN").build()).value().asBoolean());
        assertEquals(
                false,
                evaluator.evaluate(notConfig, EvaluationContext.builder().attr("country", "US").build()).value().asBoolean());
    }

    @Test
    void sameUserIsStickyAcrossCalls() {
        FlagConfig config = booleanFlag(
                "checkout",
                false,
                List.of(),
                new PercentageRollout(50, null, FlagValue.ofBoolean(true)));
        EvaluationContext user = context("sticky-user");

        boolean first = evaluator.evaluate(config, user).value().asBoolean();
        for (int i = 0; i < 20; i++) {
            assertEquals(first, evaluator.evaluate(config, user).value().asBoolean());
        }
    }

    @Test
    void percentageZeroNeverInAndHundredAlwaysInWhenIdentityPresent() {
        FlagConfig none = booleanFlag("checkout", false, List.of(), new PercentageRollout(0, null, FlagValue.ofBoolean(true)));
        FlagConfig all = booleanFlag("checkout", false, List.of(), new PercentageRollout(100, null, FlagValue.ofBoolean(true)));

        assertEquals(false, evaluator.evaluate(none, context("u1")).value().asBoolean());
        assertEquals(true, evaluator.evaluate(all, context("u1")).value().asBoolean());
    }

    @Test
    void missingBucketIdentityReturnsDefault() {
        FlagConfig config = booleanFlag(
                "checkout",
                false,
                List.of(),
                new PercentageRollout(100, null, FlagValue.ofBoolean(true)));

        EvaluationResult result = evaluator.evaluate(config, EvaluationContext.builder().attr("country", "IN").build());

        assertEquals(false, result.value().asBoolean());
        assertEquals(EvaluationReason.MISSING_BUCKET_KEY, result.reason());
    }

    @Test
    void independentFlagsCanDivergeForSameUser() {
        String userId = findUserWithDifferentIndependentBuckets();
        assertNotNull(userId);

        int bucketA = bucketer.bucket(bucketer.hashInput("flagA", userId, null));
        int bucketB = bucketer.bucket(bucketer.hashInput("flagB", userId, null));
        assertNotEquals(bucketA, bucketB);

        int threshold = Math.min(bucketA, bucketB) + 1;
        FlagConfig flagA = booleanFlag("flagA", false, List.of(), new PercentageRollout(threshold, null, FlagValue.ofBoolean(true)));
        FlagConfig flagB = booleanFlag("flagB", false, List.of(), new PercentageRollout(threshold, null, FlagValue.ofBoolean(true)));

        boolean aIn = evaluator.evaluate(flagA, context(userId)).value().asBoolean();
        boolean bIn = evaluator.evaluate(flagB, context(userId)).value().asBoolean();
        assertNotEquals(aIn, bIn);
    }

    @Test
    void sharedBucketingKeyAlignsFlags() {
        PercentageRollout sharedA = new PercentageRollout(40, "exp-2026", FlagValue.ofBoolean(true));
        PercentageRollout sharedB = new PercentageRollout(40, "exp-2026", FlagValue.ofBoolean(true));
        FlagConfig flagA = booleanFlag("flagA", false, List.of(), sharedA);
        FlagConfig flagB = booleanFlag("flagB", false, List.of(), sharedB);
        EvaluationContext user = context("shared-user");

        assertEquals(
                evaluator.evaluate(flagA, user).value().asBoolean(),
                evaluator.evaluate(flagB, user).value().asBoolean());
        assertEquals(
                bucketer.bucket(bucketer.hashInput("flagA", "shared-user", "exp-2026")),
                bucketer.bucket(bucketer.hashInput("flagB", "shared-user", "exp-2026")));
    }

    @Test
    void matchingRuleOverridesRollout() {
        TargetingRule india = new TargetingRule("country", Operator.EQUALS, "IN", FlagValue.ofBoolean(true));
        FlagConfig config = booleanFlag(
                "checkout",
                false,
                List.of(india),
                new PercentageRollout(0, null, FlagValue.ofBoolean(true)));

        EvaluationResult result = evaluator.evaluate(
                config,
                EvaluationContext.builder().userId("u1").attr("country", "IN").build());

        assertEquals(true, result.value().asBoolean());
        assertEquals(EvaluationReason.RULE_MATCH, result.reason());
    }

    private String findUserWithDifferentIndependentBuckets() {
        for (int i = 0; i < 10_000; i++) {
            String id = "u" + i;
            if (bucketer.bucket(bucketer.hashInput("flagA", id, null))
                    != bucketer.bucket(bucketer.hashInput("flagB", id, null))) {
                return id;
            }
        }
        return null;
    }

    private static EvaluationContext context(String userId) {
        return EvaluationContext.builder().userId(userId).build();
    }

    private static FlagConfig booleanFlag(
            String name,
            boolean defaultValue,
            List<TargetingRule> rules,
            PercentageRollout rollout) {
        return FlagConfig.builder()
                .name(name)
                .environment("prod")
                .valueType(FlagValueType.BOOLEAN)
                .defaultValue(FlagValue.ofBoolean(defaultValue))
                .rules(rules)
                .rollout(rollout)
                .build();
    }

    private static FlagConfig stringFlag(String defaultValue) {
        return FlagConfig.builder()
                .name("theme")
                .environment("prod")
                .valueType(FlagValueType.STRING)
                .defaultValue(FlagValue.ofString(defaultValue))
                .build();
    }

    private static FlagConfig intFlag(int defaultValue) {
        return FlagConfig.builder()
                .name("limit")
                .environment("prod")
                .valueType(FlagValueType.INTEGER)
                .defaultValue(FlagValue.ofInteger(defaultValue))
                .build();
    }
}
