package io.split.android.client.factory;

import io.split.android.client.SplitFactory;

public class FactoryMonitorImpl implements FactoryMonitor {

    private FactoryRegistry factoryRegistry;

    @Override
    public int allCount() {
        return factoryRegistry.count();
    }

    @Override
    public int instanceCount(String apiKey) {
        return factoryRegistry.count(apiKey);
    }

    @Override
    public void register(SplitFactory instance, String apiKey) {
        factoryRegistry.append(new WeakFactory(instance), apiKey);
    }
}
