package featureflag.config;

public class InvalidFlagConfigException extends RuntimeException {
    public InvalidFlagConfigException(String message) {
        super(message);
    }
}
