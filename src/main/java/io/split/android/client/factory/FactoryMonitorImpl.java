package io.split.android.client.factory;

import io.split.android.client.SplitFactory;

public class FactoryMonitorImpl implements FactoryMonitor {

    @Override
    public int allCount() {
        return 0;
    }

    @Override
    public int instanceCount(String apiKey) {
        return 0;
    }

    @Override
    public void register(SplitFactory instance, String apiKey) {

    }
}
