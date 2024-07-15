package io.split.android.client.shared;

import androidx.annotation.Nullable;

import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;

public interface ClientComponentsRegister {
    void registerComponents(Key key, SplitEventsManager eventsManager, MySegmentsTaskFactory mySegmentsTaskFactory,
                            @Nullable MySegmentsTaskFactory myLargeSegmentsTaskFactory);

    void unregisterComponentsForKey(Key key);
}
