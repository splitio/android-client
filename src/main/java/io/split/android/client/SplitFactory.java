package io.split.android.client;

import io.split.android.client.api.Key;
import io.split.android.client.shared.UserConsent;

public interface SplitFactory {
    SplitClient client();

    SplitClient client(Key key);

    SplitClient client(String matchingKey);

    SplitClient client(String matchingKey, String bucketingKey);

    SplitManager manager();

    void destroy();

    void flush();

    void setUserConsent(boolean enabled);

    UserConsent getUserConsent();

    /**
     * Deprecated: Use {@link SplitClient#isReady()}
     *
     * @return Whether at least one client instance is ready.
     */
    @Deprecated
    boolean isReady();
}
