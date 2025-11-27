package io.split.android.client.api;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class Key {
    private final String mMatchingKey;
    private final String mBucketingKey;

    public Key(String matchingKey, String bucketingKey) {
        mMatchingKey = matchingKey;
        mBucketingKey = bucketingKey;
    }

    public Key(String matchingKey) {
        this(matchingKey, null);
    }

    public String matchingKey() {
        return mMatchingKey;
    }

    public String bucketingKey() {
        return mBucketingKey;
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
        return mMatchingKey.equals(other.mMatchingKey) &&
                (Objects.equals(mBucketingKey, other.mBucketingKey));
    }

    @Override
    public int hashCode() {
        int result = 17;

        result *= 1000003;
        result ^= mMatchingKey.hashCode();

        result *= 1000003;
        if (mBucketingKey != null) {
            result ^= mBucketingKey.hashCode();
        }

        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return mMatchingKey + ", " + mBucketingKey;
    }
}
