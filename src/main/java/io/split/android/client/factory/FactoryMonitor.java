package io.split.android.client.factory;

public interface FactoryMonitor {
    int count();
    int count(String apiKey);
    void add(String apiKey);
    void remove(String apiKey);
}
