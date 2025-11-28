package io.split.android.client.events.executors;

import io.split.android.client.SplitClient;

public interface SplitEventExecutorResources {
    void setSplitClient(SplitClient client);

    SplitClient getSplitClient();
}
