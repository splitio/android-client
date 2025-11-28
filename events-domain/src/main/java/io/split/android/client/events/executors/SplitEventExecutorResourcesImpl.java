package io.split.android.client.events.executors;

import static java.util.Objects.requireNonNull;

import io.split.android.client.SplitClient;

/**
 * Created by sarrubia on 4/6/18.
 */

public class SplitEventExecutorResourcesImpl implements SplitEventExecutorResources {

    private SplitClient mClient;

    @Override
    public void setSplitClient(SplitClient client) {
        mClient = requireNonNull(client);
    }

    @Override
    public SplitClient getSplitClient() {
        return mClient;
    }
}
