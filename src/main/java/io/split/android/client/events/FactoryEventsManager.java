package io.split.android.client.events;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import io.split.android.client.utils.Logger;

/**
 * Special case event manager which handles events that should be shared among all client instances.
 */
public class FactoryEventsManager extends BaseEventsManager implements ISplitEventsManager, EventsManagerRegister {

    private final ConcurrentMap<String, ISplitEventsManager> children = new ConcurrentHashMap<>();

    public FactoryEventsManager() {
        super();

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-FactoryEventsManager-%d")
                .setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        Logger.e("Unexpected error " + e.getLocalizedMessage());
                    }
                })
                .build();

        ScheduledExecutorService mScheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);

        mScheduler.submit(this);
    }

    @Override
    public void notifyInternalEvent(SplitInternalEvent internalEvent) {
        checkNotNull(internalEvent);
        try {
            _queue.add(internalEvent);
        } catch (IllegalStateException e) {
            Logger.d("Internal events queue is full");
        }
    }

    @Override
    public boolean eventAlreadyTriggered(SplitEvent event) {
        return false;
    }

    @Override
    protected void triggerEventsWhenAreAvailable() {
        try {
            SplitInternalEvent event = _queue.take(); //Blocking method (waiting if necessary until an element becomes available.)
            _triggered.add(event);
            switch (event) {
                case SPLITS_UPDATED:
                case SPLITS_FETCHED:
                case SPLITS_LOADED_FROM_STORAGE:
                case SPLIT_KILLED_NOTIFICATION:
                    for (ISplitEventsManager child : children.values()) {
                        child.notifyInternalEvent(event);
                    }
                    break;
            }
        } catch (InterruptedException e) {
            //Catching the InterruptedException that can be thrown by _queue.take() if interrupted while waiting
            // for further information read https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ArrayBlockingQueue.html#take()
            Logger.d(e.getMessage());
        }
    }

    @Override
    public void registerEventsManager(String matchingKey, ISplitEventsManager splitEventsManager) {
        children.put(matchingKey, splitEventsManager);

        // Inform the new events manager of any events notified prior to registration
        propagateTriggeredEvents(splitEventsManager);
    }

    @Override
    public void unregisterEventsManager(String matchingKey) {
        children.remove(matchingKey);
    }

    private void propagateTriggeredEvents(ISplitEventsManager splitEventsManager) {
        for (SplitInternalEvent event : _triggered) {
            splitEventsManager.notifyInternalEvent(event);
        }
    }

}
