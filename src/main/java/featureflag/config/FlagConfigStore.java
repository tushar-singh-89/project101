package featureflag.config;

import featureflag.model.FlagConfig;

import java.util.Optional;

public interface FlagConfigStore {
    void set(FlagConfig config);

    Optional<FlagConfig> get(String name, String environment);

    void addListener(ConfigChangeListener listener);
}
