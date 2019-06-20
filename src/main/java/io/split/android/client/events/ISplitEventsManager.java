package io.split.android.client.events;

import io.split.android.client.events.executors.SplitEventExecutorResources;

public interface ISplitEventsManager {
    SplitEventExecutorResources getExecutorResources();
    void notifyInternalEvent(SplitInternalEvent internalEvent);
    void register(SplitEvent event, SplitEventTask task);
    boolean eventAlreadyTriggered(SplitEvent event);
}
