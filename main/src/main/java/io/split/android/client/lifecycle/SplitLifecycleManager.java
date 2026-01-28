package io.split.android.client.lifecycle;

public interface SplitLifecycleManager {
    void register(SplitLifecycleAware component);

    void destroy();
}
