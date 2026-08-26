package featureflag.config;

import featureflag.model.FlagConfig;
import featureflag.model.FlagValue;
import featureflag.model.FlagValueType;
import featureflag.model.Operator;
import featureflag.model.TargetingRule;

import java.util.List;

final class FlagConfigValidator {
    void validate(FlagConfig config) {
        if (config == null) {
            throw new InvalidFlagConfigException("config is required");
        }
        requireNonBlank(config.name(), "name");
        requireNonBlank(config.environment(), "environment");
        requireType(config.defaultValue(), config.valueType(), "defaultValue");
        int index = 0;
        for (TargetingRule rule : config.rules()) {
            if (rule == null) {
                throw new InvalidFlagConfigException("rule[" + index + "] is required");
            }
            requireNonBlank(rule.attribute(), "rule[" + index + "].attribute");
            requireType(rule.servedValue(), config.valueType(), "rule[" + index + "].servedValue");
            validateOperand(rule, index);
            index++;
        }
        config.rollout().ifPresent(rollout -> {
            if (rollout.percentage() < 0 || rollout.percentage() > 100) {
                throw new InvalidFlagConfigException("percentage must be between 0 and 100");
            }
            requireType(rollout.rolloutValue(), config.valueType(), "rolloutValue");
            rollout.sharedBucketingKey().ifPresent(key -> requireNonBlank(key, "sharedBucketingKey"));
        });
    }

    private static void validateOperand(TargetingRule rule, int index) {
        Object operand = rule.operand();
        if (rule.operator() == Operator.IN) {
            if (!(operand instanceof List<?>) || ((List<?>) operand).isEmpty()) {
                throw new InvalidFlagConfigException("rule[" + index + "] IN operand must be a non-empty list");
            }
            for (Object item : (List<?>) operand) {
                if (item == null) {
                    throw new InvalidFlagConfigException("rule[" + index + "] IN operand cannot contain null");
                }
            }
            return;
        }
        if (operand instanceof List<?>) {
            throw new InvalidFlagConfigException("rule[" + index + "] operand must be a scalar");
        }
    }

    private static void requireType(FlagValue value, FlagValueType expected, String field) {
        if (value == null || !value.hasType(expected)) {
            throw new InvalidFlagConfigException(field + " must have type " + expected);
        }
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidFlagConfigException(field + " is required");
        }
    }
}
