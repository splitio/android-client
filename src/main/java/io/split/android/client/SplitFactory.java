package io.split.android.client;

import io.split.android.client.api.Key;

public interface SplitFactory {
    SplitClient client();
    SplitManager manager();
    void destroy();
    void flush();
    boolean isReady();
}
