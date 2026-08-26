package featureflag.evaluation;

public enum EvaluationReason {
    RULE_MATCH,
    PERCENTAGE_IN,
    PERCENTAGE_OUT,
    DEFAULT,
    MISSING_BUCKET_KEY
}
