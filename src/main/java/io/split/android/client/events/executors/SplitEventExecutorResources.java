package io.split.android.client.events.executors;

import static com.google.common.base.Preconditions.checkNotNull;

import io.split.android.client.SplitClient;

/**
 * Created by sarrubia on 4/6/18.
 */

public class SplitEventExecutorResources {

    private SplitClient _client;


    public void setSplitClient(SplitClient client) {
        checkNotNull(client);

        _client = client;
    }

    public SplitClient getSplitClient() {
        return _client;
    }
}
