package featureflag.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EvaluationContext {
    private final String userId;
    private final String anonymousId;
    private final String tenantId;
    private final Map<String, Object> attributes;

    private EvaluationContext(
            String userId,
            String anonymousId,
            String tenantId,
            Map<String, Object> attributes) {
        this.userId = blankToNull(userId);
        this.anonymousId = blankToNull(anonymousId);
        this.tenantId = blankToNull(tenantId);
        this.attributes = Map.copyOf(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> userId() {
        return Optional.ofNullable(userId);
    }

    public Optional<String> anonymousId() {
        return Optional.ofNullable(anonymousId);
    }

    public Optional<String> tenantId() {
        return Optional.ofNullable(tenantId);
    }

    public Optional<Object> attribute(String name) {
        Objects.requireNonNull(name, "name");
        Object fromMap = attributes.get(name);
        if (fromMap != null) {
            return Optional.of(fromMap);
        }
        switch (name) {
            case "userId":
                return Optional.ofNullable(userId);
            case "anonymousId":
                return Optional.ofNullable(anonymousId);
            case "tenantId":
                return Optional.ofNullable(tenantId);
            default:
                return Optional.empty();
        }
    }

    public Optional<String> bucketIdentity() {
        if (userId != null) {
            return Optional.of(userId);
        }
        if (anonymousId != null) {
            return Optional.of(anonymousId);
        }
        return Optional.empty();
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public static final class Builder {
        private String userId;
        private String anonymousId;
        private String tenantId;
        private final Map<String, Object> attributes = new HashMap<>();

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder anonymousId(String anonymousId) {
            this.anonymousId = anonymousId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder attr(String name, Object value) {
            Objects.requireNonNull(name, "name");
            if (value != null) {
                attributes.put(name, value);
            }
            return this;
        }

        public EvaluationContext build() {
            return new EvaluationContext(userId, anonymousId, tenantId, attributes);
        }
    }
}
