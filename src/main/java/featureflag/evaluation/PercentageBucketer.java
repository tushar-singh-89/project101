package featureflag.evaluation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

final class PercentageBucketer {
    int bucket(String hashInput) {
        Objects.requireNonNull(hashInput, "hashInput");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(hashInput.getBytes(StandardCharsets.UTF_8));
            long value = 0L;
            for (int i = 0; i < 8; i++) {
                value = (value << 8) | (hash[i] & 0xffL);
            }
            return (int) Long.remainderUnsigned(value, 100L);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    String hashInput(String flagName, String identity, String sharedBucketingKey) {
        String salt = sharedBucketingKey == null ? flagName : sharedBucketingKey;
        return salt + ":" + identity;
    }
}
