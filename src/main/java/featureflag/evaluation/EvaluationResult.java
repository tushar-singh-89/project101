package featureflag.evaluation;

import featureflag.model.FlagValue;

public final class EvaluationResult {
    private final FlagValue value;
    private final EvaluationReason reason;

    EvaluationResult(FlagValue value, EvaluationReason reason) {
        this.value = value;
        this.reason = reason;
    }

    public FlagValue value() {
        return value;
    }

    public EvaluationReason reason() {
        return reason;
    }
}
