package io.split.android.fake;

import androidx.annotation.Nullable;

import io.split.android.client.events.metadata.EventMetadata;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.ListenableEventsManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.events.executors.SplitEventExecutorResources;

public class SplitEventsManagerStub implements ISplitEventsManager, ListenableEventsManager {

    public boolean isSdkReadyTriggered = true;

    @Override
    public SplitEventExecutorResources getExecutorResources() {
        return null;
    }

    @Override
    public void notifyInternalEvent(SplitInternalEvent internalEvent) {
        notifyInternalEvent(internalEvent, null);
    }

    @Override
    public void notifyInternalEvent(SplitInternalEvent internalEvent, @Nullable EventMetadata metadata) {
        // Stub implementation - does nothing
    }

    @Override
    public void register(SplitEvent event, SplitEventTask task) {

    }

    @Override
    public boolean eventAlreadyTriggered(SplitEvent event) {
        if (event == SplitEvent.SDK_READY) {
            return isSdkReadyTriggered;
        }
        return false;
    }
}
