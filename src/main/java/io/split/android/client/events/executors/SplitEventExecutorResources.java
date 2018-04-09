package io.split.android.client.events.executors;

import io.split.android.client.SplitClient;

/**
 * Created by sarrubia on 4/6/18.
 */

public class SplitEventExecutorResources {

    private SplitClient _client;


    public void setSplitClient(SplitClient client) {
        _client = client;
    }

    public SplitClient getSplitClient() {
        return _client;
    }
}
