package io.split.android.client.factory;

import io.split.android.client.SplitFactory;

public interface FactoryMonitor {
    int allCount();
    int instanceCount(String apiKey);
    void register(SplitFactory instance, String apiKey);
}
