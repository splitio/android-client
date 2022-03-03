package io.split.android.client.events;

public interface ISplitEventsManager {

    void notifyInternalEvent(SplitInternalEvent internalEvent);

    boolean eventAlreadyTriggered(SplitEvent event);
}
