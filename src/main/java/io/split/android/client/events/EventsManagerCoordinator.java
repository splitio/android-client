package io.split.android.client.events;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.split.android.client.api.Key;
import io.split.android.client.utils.Logger;

/**
 * Special case event manager which handles events that should be shared among all client instances.
 */
public class EventsManagerCoordinator extends BaseEventsManager implements ISplitEventsManager, EventsManagerRegistry {

    private final ConcurrentMap<Key, ISplitEventsManager> mChildren = new ConcurrentHashMap<>();
    private final Object mEventLock = new Object();

    @Override
    public void notifyInternalEvent(SplitInternalEvent internalEvent) {
        checkNotNull(internalEvent);
        try {
            mQueue.add(internalEvent);
        } catch (IllegalStateException e) {
            Logger.d("Internal events queue is full");
        }
    }

    @Override
    protected void triggerEventsWhenAreAvailable() {
        try {
            SplitInternalEvent event = mQueue.take(); //Blocking method (waiting if necessary until an element becomes available.)
            synchronized (mEventLock) {
                mTriggered.add(event);
                switch (event) {
                    case SPLITS_UPDATED:
                    case SPLITS_FETCHED:
                    case SPLITS_LOADED_FROM_STORAGE:
                    case SPLIT_KILLED_NOTIFICATION:
                        for (ISplitEventsManager child : mChildren.values()) {
                            child.notifyInternalEvent(event);
                        }
                        break;
                }
            }
        } catch (InterruptedException e) {
            //Catching the InterruptedException that can be thrown by _queue.take() if interrupted while waiting
            // for further information read https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ArrayBlockingQueue.html#take()
            Logger.d(e.getMessage());
        }
    }

    @Override
    public void registerEventsManager(Key key, ISplitEventsManager splitEventsManager) {
        mChildren.put(key, splitEventsManager);

        // Inform the newly registered events manager of any events that occurred prior to registration
        propagateTriggeredEvents(splitEventsManager);
    }

    @Override
    public void unregisterEventsManager(Key key) {
        mChildren.remove(key);
    }

    private void propagateTriggeredEvents(ISplitEventsManager splitEventsManager) {
        synchronized (mEventLock) {
            for (SplitInternalEvent event : mTriggered) {
                splitEventsManager.notifyInternalEvent(event);
            }
        }
    }
}
