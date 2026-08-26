package featureflag.model;

import java.util.Objects;
import java.util.Optional;

public final class PercentageRollout {
    private final int percentage;
    private final String sharedBucketingKey;
    private final FlagValue rolloutValue;

    public PercentageRollout(int percentage, String sharedBucketingKey, FlagValue rolloutValue) {
        this.percentage = percentage;
        this.sharedBucketingKey = sharedBucketingKey == null || sharedBucketingKey.isBlank()
                ? null
                : sharedBucketingKey;
        this.rolloutValue = Objects.requireNonNull(rolloutValue, "rolloutValue");
    }

    public int percentage() {
        return percentage;
    }

    public Optional<String> sharedBucketingKey() {
        return Optional.ofNullable(sharedBucketingKey);
    }

    public FlagValue rolloutValue() {
        return rolloutValue;
    }
}
