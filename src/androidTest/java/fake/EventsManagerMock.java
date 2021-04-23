package fake;

import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.events.executors.SplitEventExecutorResources;

public class EventsManagerMock implements ISplitEventsManager {
    @Override
    public SplitEventExecutorResources getExecutorResources() {
        return null;
    }

    @Override
    public void notifyInternalEvent(SplitInternalEvent internalEvent) {

    }

    @Override
    public void register(SplitEvent event, SplitEventTask task) {

    }

    @Override
    public boolean eventAlreadyTriggered(SplitEvent event) {
        return false;
    }

    public boolean wasTriggered(SplitInternalEvent event) {
        return false;
    }
}
