package featureflag.config;

import featureflag.model.FlagConfig;

@FunctionalInterface
public interface ConfigChangeListener {
    void onConfigChanged(FlagConfig config);
}
