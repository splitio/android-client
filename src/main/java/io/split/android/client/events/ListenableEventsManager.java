package io.split.android.client.events;

import io.split.android.client.events.executors.SplitEventExecutorResources;

public interface ListenableEventsManager {

    SplitEventExecutorResources getExecutorResources();

    void register(SplitEvent event, SplitEventTask task);
}
