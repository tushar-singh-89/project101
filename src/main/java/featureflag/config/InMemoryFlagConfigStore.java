package featureflag.config;

import featureflag.model.FlagConfig;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryFlagConfigStore implements FlagConfigStore {
    private final ConcurrentHashMap<FlagKey, FlagConfig> configs = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final FlagConfigValidator validator = new FlagConfigValidator();

    @Override
    public void set(FlagConfig config) {
        validator.validate(config);
        FlagKey key = new FlagKey(config.name(), config.environment());
        configs.put(key, config);
        for (ConfigChangeListener listener : listeners) {
            listener.onConfigChanged(config);
        }
    }

    @Override
    public Optional<FlagConfig> get(String name, String environment) {
        if (name == null || environment == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(configs.get(new FlagKey(name, environment)));
    }

    @Override
    public void addListener(ConfigChangeListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    private static final class FlagKey {
        private final String name;
        private final String environment;

        private FlagKey(String name, String environment) {
            this.name = name;
            this.environment = environment;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FlagKey)) {
                return false;
            }
            FlagKey flagKey = (FlagKey) o;
            return Objects.equals(name, flagKey.name) && Objects.equals(environment, flagKey.environment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, environment);
        }
    }
}
