package featureflag.evaluation;

import featureflag.model.EvaluationContext;
import featureflag.model.FlagConfig;
import featureflag.model.PercentageRollout;

import java.util.Optional;

public class FlagEvaluator {
    private final RuleMatcher ruleMatcher;
    private final PercentageBucketer bucketer;

    public FlagEvaluator() {
        this(new RuleMatcher(), new PercentageBucketer());
    }

    FlagEvaluator(RuleMatcher ruleMatcher, PercentageBucketer bucketer) {
        this.ruleMatcher = ruleMatcher;
        this.bucketer = bucketer;
    }

    public EvaluationResult evaluate(FlagConfig config, EvaluationContext context) {
        return ruleMatcher.firstMatch(config, context)
                .map(value -> new EvaluationResult(value, EvaluationReason.RULE_MATCH))
                .orElseGet(() -> evaluateFallthrough(config, context));
    }

    private EvaluationResult evaluateFallthrough(FlagConfig config, EvaluationContext context) {
        if (config.rollout().isEmpty()) {
            return new EvaluationResult(config.defaultValue(), EvaluationReason.DEFAULT);
        }
        PercentageRollout rollout = config.rollout().get();
        Optional<String> identity = context.bucketIdentity();
        if (identity.isEmpty()) {
            return new EvaluationResult(config.defaultValue(), EvaluationReason.MISSING_BUCKET_KEY);
        }
        String hashInput = bucketer.hashInput(
                config.name(),
                identity.get(),
                rollout.sharedBucketingKey().orElse(null));
        int bucket = bucketer.bucket(hashInput);
        if (bucket < rollout.percentage()) {
            return new EvaluationResult(rollout.rolloutValue(), EvaluationReason.PERCENTAGE_IN);
        }
        return new EvaluationResult(config.defaultValue(), EvaluationReason.PERCENTAGE_OUT);
    }
}
