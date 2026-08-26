package featureflag.client;

import java.util.Map;

public interface FlagErrorLogger {
    void error(String code, Map<String, String> fields, Throwable cause);
}
