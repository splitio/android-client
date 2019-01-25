package io.split.android.client.api;

import io.split.android.client.validators.Validatable;
import io.split.android.client.validators.Validator;

public final class Key implements Validatable<Key> {
    private final String _matchingKey;
    private final String _bucketingKey;

    public static Key withMatchingKey(String matchingKey) {
        return new Key(matchingKey, null);
    }

    public static Key withMatchingKeyAndBucketingKey(String matchingKey, String bucketingKey) {
        return new Key(matchingKey, bucketingKey);
    }

    public Key(String matchingKey, String bucketingKey) {
        _matchingKey = matchingKey;
        _bucketingKey = bucketingKey;
    }

    public String matchingKey() {
        return _matchingKey;
    }

    public String bucketingKey() {
        return _bucketingKey;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (!(o instanceof Key)) {
            return false;
        }

        Key other = (Key) o;
        return _matchingKey.equals(other._matchingKey) &&
                _bucketingKey.equals(other._bucketingKey);
    }

    @Override
    public int hashCode() {
        int result = 17;


        result *= 1000003;
        result ^= _matchingKey.hashCode();

        result *= 1000003;
        result ^= _bucketingKey.hashCode();

        return result;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append(_matchingKey);
        bldr.append(", ");
        bldr.append(_bucketingKey);
        return bldr.toString();
    }

    @Override
    public Boolean isValid(Validator validator) {
        return validator.isValidEntity(this);
    }
}
