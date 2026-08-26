package featureflag.model;

import java.util.List;
import java.util.Objects;

public final class TargetingRule {
    private final String attribute;
    private final Operator operator;
    private final Object operand;
    private final FlagValue servedValue;

    public TargetingRule(String attribute, Operator operator, Object operand, FlagValue servedValue) {
        this.attribute = Objects.requireNonNull(attribute, "attribute");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.operand = Objects.requireNonNull(operand, "operand");
        this.servedValue = Objects.requireNonNull(servedValue, "servedValue");
        if (operator == Operator.IN) {
            if (!(operand instanceof List<?>) || ((List<?>) operand).isEmpty()) {
                throw new IllegalArgumentException("IN operand must be a non-empty list");
            }
        }
    }

    public String attribute() {
        return attribute;
    }

    public Operator operator() {
        return operator;
    }

    public Object operand() {
        return operand;
    }

    public FlagValue servedValue() {
        return servedValue;
    }
}
