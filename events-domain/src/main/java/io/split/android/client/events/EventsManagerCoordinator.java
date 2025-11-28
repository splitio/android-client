package io.split.android.client.events;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.split.android.client.api.Key;

/**
 * Coordinator for SDK-scoped events that should be propagated to all client event managers.
 * <p>
 * This coordinator keeps track of all registered {@link ISplitEventsManager} instances
 * and forwards SDK-scoped internal events (like splits updates) to all of them.
 * <p>
 * Client-scoped events (like segments updates for a specific key) should be sent
 * directly to the corresponding client's event manager.
 */
public class EventsManagerCoordinator implements ISplitEventsManager, EventsManagerRegistry {

    /**
     * Set of SDK-scoped internal events that should be propagated to all registered managers.
     */
    private static final Set<SplitInternalEvent> SDK_SCOPED_EVENTS = EnumSet.of(
            SplitInternalEvent.SPLITS_UPDATED,
            SplitInternalEvent.SPLITS_FETCHED,
            SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE,
            SplitInternalEvent.SPLIT_KILLED_NOTIFICATION,
            SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED,
            SplitInternalEvent.ENCRYPTION_MIGRATION_DONE
    );

    private final ConcurrentMap<Key, ISplitEventsManager> mManagers = new ConcurrentHashMap<>();
    private final Set<SplitInternalEvent> mTriggeredEvents = ConcurrentHashMap.newKeySet();
    private final Object mLock = new Object();

    /**
     * Notifies an SDK-scoped internal event.
     * <p>
     * If the event is SDK-scoped (like splits updates), it will be propagated
     * to all registered event managers. Client-scoped events are ignored and should
     * be sent directly to the corresponding client's event manager.
     *
     * @param internalEvent the internal event to notify
     */
    @Override
    public void notifyInternalEvent(SplitInternalEvent internalEvent) {
        requireNonNull(internalEvent);

        if (!SDK_SCOPED_EVENTS.contains(internalEvent)) {
            // Client-scoped events should be sent directly to the client's manager
            return;
        }

        synchronized (mLock) {
            mTriggeredEvents.add(internalEvent);

            // Fan out to all registered managers
            for (ISplitEventsManager manager : getManagersSnapshot()) {
                manager.notifyInternalEvent(internalEvent);
            }
        }
    }

    /**
     * Registers an events manager for a client key.
     * <p>
     * Any SDK-scoped events that occurred prior to registration will be propagated
     * to the newly registered manager.
     *
     * @param key                 the client key
     * @param splitEventsManager  the events manager for that client
     */
    @Override
    public void registerEventsManager(Key key, ISplitEventsManager splitEventsManager) {
        requireNonNull(key);
        requireNonNull(splitEventsManager);

        mManagers.put(key, splitEventsManager);

        // Propagate any events that occurred before registration
        propagateTriggeredEvents(splitEventsManager);
    }

    /**
     * Unregisters the events manager for a client key.
     *
     * @param key the client key to unregister
     */
    @Override
    public void unregisterEventsManager(Key key) {
        if (key != null) {
            mManagers.remove(key);
        }
    }

    /**
     * Notifies a client-scoped internal event to a specific client.
     * <p>
     * Use this method for events that are specific to a single client,
     * such as segments updates.
     *
     * @param key           the client key
     * @param internalEvent the internal event to notify
     */
    public void notifyClientScopedEvent(Key key, SplitInternalEvent internalEvent) {
        requireNonNull(key);
        requireNonNull(internalEvent);

        ISplitEventsManager manager = mManagers.get(key);
        if (manager != null) {
            manager.notifyInternalEvent(internalEvent);
        }
    }

    /**
     * Gets the events manager for a specific client key.
     *
     * @param key the client key
     * @return the events manager, or null if not registered
     */
    public ISplitEventsManager getEventsManager(Key key) {
        return mManagers.get(key);
    }

    /**
     * Returns the number of registered event managers.
     *
     * @return the count of registered managers
     */
    public int getRegisteredCount() {
        return mManagers.size();
    }

    // --- Private methods ---

    private void propagateTriggeredEvents(ISplitEventsManager splitEventsManager) {
        synchronized (mLock) {
            for (SplitInternalEvent event : mTriggeredEvents) {
                splitEventsManager.notifyInternalEvent(event);
            }
        }
    }

    private Collection<ISplitEventsManager> getManagersSnapshot() {
        // ConcurrentHashMap.values() returns a view that is weakly consistent,
        // which is sufficient for our iteration needs
        return mManagers.values();
    }
}
