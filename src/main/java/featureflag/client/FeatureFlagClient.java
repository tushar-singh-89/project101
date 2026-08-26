package featureflag.client;

import featureflag.config.FlagConfigStore;
import featureflag.evaluation.EvaluationReason;
import featureflag.evaluation.EvaluationResult;
import featureflag.evaluation.FlagEvaluator;
import featureflag.model.EvaluationContext;
import featureflag.model.FlagConfig;
import featureflag.model.FlagValue;
import featureflag.model.FlagValueType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class FeatureFlagClient {
    private final FlagConfigStore store;
    private final String environment;
    private final FlagErrorLogger logger;
    private final FlagEvaluator evaluator;

    public FeatureFlagClient(FlagConfigStore store, String environment, FlagErrorLogger logger) {
        this(store, environment, logger, new FlagEvaluator());
    }

    public FeatureFlagClient(
            FlagConfigStore store,
            String environment,
            FlagErrorLogger logger,
            FlagEvaluator evaluator) {
        this.store = store;
        this.environment = environment;
        this.logger = logger;
        this.evaluator = evaluator;
    }

    public boolean getBoolean(String name, EvaluationContext context, boolean fallback) {
        return unwrap(name, context, FlagValueType.BOOLEAN, FlagValue.ofBoolean(fallback)).asBoolean();
    }

    public String getString(String name, EvaluationContext context, String fallback) {
        String safeFallback = fallback == null ? "" : fallback;
        return unwrap(name, context, FlagValueType.STRING, FlagValue.ofString(safeFallback)).asString();
    }

    public int getInteger(String name, EvaluationContext context, int fallback) {
        return unwrap(name, context, FlagValueType.INTEGER, FlagValue.ofInteger(fallback)).asInteger();
    }

    private FlagValue unwrap(
            String name,
            EvaluationContext context,
            FlagValueType expectedType,
            FlagValue fallback) {
        try {
            if (name == null || name.isBlank() || context == null) {
                log("INVALID_INPUT", name, null);
                return fallback;
            }
            Optional<FlagConfig> maybeConfig = store.get(name, environment);
            if (maybeConfig.isEmpty()) {
                log("FLAG_NOT_FOUND", name, null);
                return fallback;
            }
            FlagConfig config = maybeConfig.get();
            if (config.valueType() != expectedType) {
                log("TYPE_MISMATCH", name, null);
                return compatibleDefault(config, expectedType, fallback);
            }
            EvaluationResult result = evaluator.evaluate(config, context);
            if (result.reason() == EvaluationReason.MISSING_BUCKET_KEY) {
                log("MISSING_BUCKET_KEY", name, null);
            }
            FlagValue value = result.value();
            if (!value.hasType(expectedType)) {
                log("TYPE_MISMATCH", name, null);
                return compatibleDefault(config, expectedType, fallback);
            }
            return value;
        } catch (RuntimeException e) {
            log("EVAL_ERROR", name, e);
            try {
                return store.get(name, environment)
                        .map(config -> compatibleDefault(config, expectedType, fallback))
                        .orElse(fallback);
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
    }

    private FlagValue compatibleDefault(FlagConfig config, FlagValueType expectedType, FlagValue fallback) {
        if (config.defaultValue().hasType(expectedType)) {
            return config.defaultValue();
        }
        return fallback;
    }

    private void log(String code, String name, Throwable cause) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("flag", name == null ? "" : name);
        fields.put("env", environment);
        fields.put("code", code);
        logger.error(code, fields, cause);
    }
}
