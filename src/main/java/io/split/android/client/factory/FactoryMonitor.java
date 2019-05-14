package io.split.android.client.factory;

import io.split.android.client.SplitFactory;

public interface FactoryMonitor {
    int count();
    int count(String apiKey);
    void add(String apiKey);
    void remove(String apiKey);
}
