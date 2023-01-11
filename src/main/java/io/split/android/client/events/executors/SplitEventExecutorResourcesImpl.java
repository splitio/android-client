package io.split.android.client.events.executors;

import static com.google.common.base.Preconditions.checkNotNull;

import io.split.android.client.SplitClient;

/**
 * Created by sarrubia on 4/6/18.
 */

public class SplitEventExecutorResourcesImpl implements SplitEventExecutorResources {

    private SplitClient mClient;

    @Override
    public void setSplitClient(SplitClient client) {
        mClient = checkNotNull(client);
    }

    @Override
    public SplitClient getSplitClient() {
        return mClient;
    }
}
