package io.split.android.client.events;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.executors.SplitEventExecutorResources;

/**
 * TODO: implemented in following PR.
 *
 * Takes care of handling events common to all clients.
 */
public class FactoryEventsManager implements ISplitEventsManager, EventsManagerRegistry {
    public FactoryEventsManager(SplitClientConfig config) {

    }

    @Override
    public SplitEventExecutorResources getExecutorResources() {
        return null; //TODO
    }

    @Override
    public void notifyInternalEvent(SplitInternalEvent internalEvent) {
        // TODO
    }

    @Override
    public void register(SplitEvent event, SplitEventTask task) {
        // TODO
    }

    @Override
    public boolean eventAlreadyTriggered(SplitEvent event) {
        return false;
    }

    @Override
    public void registerEventsManager(String matchingKey, ISplitEventsManager splitEventsManager) {

    }

    @Override
    public void unregisterEventsManager(String matchingKey) {

    }
}
