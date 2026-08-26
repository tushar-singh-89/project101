package featureflag.evaluation;

import featureflag.model.EvaluationContext;
import featureflag.model.FlagConfig;
import featureflag.model.FlagValue;
import featureflag.model.TargetingRule;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class RuleMatcher {
    Optional<FlagValue> firstMatch(FlagConfig config, EvaluationContext context) {
        for (TargetingRule rule : config.rules()) {
            if (matches(rule, context)) {
                return Optional.of(rule.servedValue());
            }
        }
        return Optional.empty();
    }

    private boolean matches(TargetingRule rule, EvaluationContext context) {
        Optional<Object> actual = context.attribute(rule.attribute());
        if (actual.isEmpty()) {
            return false;
        }
        switch (rule.operator()) {
            case EQUALS:
                return valuesEqual(actual.get(), rule.operand());
            case NOT_EQUALS:
                return !valuesEqual(actual.get(), rule.operand());
            case IN:
                return inList(actual.get(), rule.operand());
            default:
                return false;
        }
    }

    private boolean inList(Object actual, Object operand) {
        if (!(operand instanceof List<?>)) {
            return false;
        }
        for (Object item : (List<?>) operand) {
            if (valuesEqual(actual, item)) {
                return true;
            }
        }
        return false;
    }

    private boolean valuesEqual(Object left, Object right) {
        if (Objects.equals(left, right)) {
            return true;
        }
        if (left instanceof Number && right instanceof Number) {
            Number leftNumber = (Number) left;
            Number rightNumber = (Number) right;
            return leftNumber.longValue() == rightNumber.longValue()
                    && Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue()) == 0;
        }
        return String.valueOf(left).equals(String.valueOf(right));
    }
}
