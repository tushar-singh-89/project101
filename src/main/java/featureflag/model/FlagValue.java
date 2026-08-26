package featureflag.model;

import java.util.Objects;

public final class FlagValue {
    private final FlagValueType type;
    private final Object value;

    private FlagValue(FlagValueType type, Object value) {
        this.type = Objects.requireNonNull(type, "type");
        this.value = Objects.requireNonNull(value, "value");
    }

    public static FlagValue ofBoolean(boolean value) {
        return new FlagValue(FlagValueType.BOOLEAN, value);
    }

    public static FlagValue ofString(String value) {
        return new FlagValue(FlagValueType.STRING, Objects.requireNonNull(value, "value"));
    }

    public static FlagValue ofInteger(int value) {
        return new FlagValue(FlagValueType.INTEGER, value);
    }

    public FlagValueType type() {
        return type;
    }

    public boolean asBoolean() {
        requireType(FlagValueType.BOOLEAN);
        return (Boolean) value;
    }

    public String asString() {
        requireType(FlagValueType.STRING);
        return (String) value;
    }

    public int asInteger() {
        requireType(FlagValueType.INTEGER);
        return (Integer) value;
    }

    public boolean hasType(FlagValueType expected) {
        return type == expected;
    }

    private void requireType(FlagValueType expected) {
        if (type != expected) {
            throw new IllegalStateException("Expected " + expected + " but was " + type);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlagValue)) {
            return false;
        }
        FlagValue other = (FlagValue) o;
        return type == other.type && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return type + "(" + value + ")";
    }
}
