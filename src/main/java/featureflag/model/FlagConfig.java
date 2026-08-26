package featureflag.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FlagConfig {
    private final String name;
    private final String environment;
    private final FlagValueType valueType;
    private final FlagValue defaultValue;
    private final List<TargetingRule> rules;
    private final PercentageRollout rollout;

    private FlagConfig(
            String name,
            String environment,
            FlagValueType valueType,
            FlagValue defaultValue,
            List<TargetingRule> rules,
            PercentageRollout rollout) {
        this.name = Objects.requireNonNull(name, "name");
        this.environment = Objects.requireNonNull(environment, "environment");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.rules = List.copyOf(rules);
        this.rollout = rollout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String name() {
        return name;
    }

    public String environment() {
        return environment;
    }

    public FlagValueType valueType() {
        return valueType;
    }

    public FlagValue defaultValue() {
        return defaultValue;
    }

    public List<TargetingRule> rules() {
        return rules;
    }

    public Optional<PercentageRollout> rollout() {
        return Optional.ofNullable(rollout);
    }

    public static final class Builder {
        private String name;
        private String environment;
        private FlagValueType valueType;
        private FlagValue defaultValue;
        private List<TargetingRule> rules = List.of();
        private PercentageRollout rollout;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder valueType(FlagValueType valueType) {
            this.valueType = valueType;
            return this;
        }

        public Builder defaultValue(FlagValue defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder rules(List<TargetingRule> rules) {
            this.rules = rules == null ? List.of() : rules;
            return this;
        }

        public Builder rollout(PercentageRollout rollout) {
            this.rollout = rollout;
            return this;
        }

        public FlagConfig build() {
            return new FlagConfig(name, environment, valueType, defaultValue, rules, rollout);
        }
    }
}
