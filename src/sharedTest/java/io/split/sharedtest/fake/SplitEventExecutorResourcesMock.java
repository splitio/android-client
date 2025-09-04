package io.split.sharedtest.fake;

import io.split.android.client.SplitClient;
import io.split.android.client.events.executors.SplitEventExecutorResources;

public class SplitEventExecutorResourcesMock implements SplitEventExecutorResources {
    @Override
    public void setSplitClient(SplitClient client) {

    }

    @Override
    public SplitClient getSplitClient() {
        return new SplitClientStub();
    }
}
