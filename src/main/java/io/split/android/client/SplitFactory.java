package io.split.android.client;

import io.split.android.client.api.Key;

public interface SplitFactory {
    SplitClient client();
    SplitClient client(Key key);
    SplitClient client(String matchingKey);
    SplitClient client(String matchingKey, String bucketingKey);
    SplitManager manager();
    void destroy();
    void flush();

    /**
     * Deprecated: Use {@link SplitClient#isReady()}
     *
     * @return Whether at least one client instance is ready.
     */
    @Deprecated
    boolean isReady();
}
