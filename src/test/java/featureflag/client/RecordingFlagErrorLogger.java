package featureflag.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class RecordingFlagErrorLogger implements FlagErrorLogger {
    private final List<String> codes = new ArrayList<>();

    @Override
    public void error(String code, Map<String, String> fields, Throwable cause) {
        codes.add(code);
    }

    List<String> codes() {
        return List.copyOf(codes);
    }

    boolean hasCode(String code) {
        return codes.contains(code);
    }
}
