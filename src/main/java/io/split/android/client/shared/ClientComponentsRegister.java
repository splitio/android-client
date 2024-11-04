package io.split.android.client.shared;

import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;

public interface ClientComponentsRegister {
    void registerComponents(Key key, SplitEventsManager eventsManager, MySegmentsTaskFactory mySegmentsTaskFactory);

    void unregisterComponentsForKey(Key key);
}
