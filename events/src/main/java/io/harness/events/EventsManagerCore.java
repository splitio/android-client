package io.harness.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventsManagerCore<E, I, M> implements EventsManager<E, I, M> {

    private final Map<E, List<EventHandler<E, M>>> mSubscriptions = new ConcurrentHashMap<>();
    private final Map<E, Integer> mRemainingExecutions = new ConcurrentHashMap<>();
    protected final Set<E> mFired = Collections.newSetFromMap(new ConcurrentHashMap<E, Boolean>());
    private final Set<I> mSeenInternal = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final EventsManagerConfig<E, I> mEventsManagerConfig;
    @NonNull
    private final EventDelivery<E, M> mDelivery;
    private final AtomicBoolean mRunning = new AtomicBoolean(false);

    public EventsManagerCore(@Nullable EventsManagerConfig<E, I> eventsManagerConfig, EventDelivery<E, M> delivery) {
        mEventsManagerConfig = eventsManagerConfig == null ? EventsManagerConfig.empty() : eventsManagerConfig;
        mDelivery = delivery;
    }

    @Override
    public void register(E event, EventHandler handler) {
        // If the event was already triggered, we replay it
        if (eventAlreadyTriggered(event)) {
            mDelivery.deliver(handler, event, null);
            return;
        }

        // Add new handler to the corresponding event's handlers
        List<EventHandler> list = mSubscriptions.get(event);
        if (list == null) {
            synchronized (mSubscriptions) {
                list = mSubscriptions.get(event);
                if (list == null) {
                    list = new CopyOnWriteArrayList<>();
                    mSubscriptions.put(event, list);
                }
            }
        }
        list.add(handler);

        synchronized (mRemainingExecutions) {
            if (!mRemainingExecutions.containsKey(event)) {
                mRemainingExecutions.put(event, maxExecutions(event));
            }
        }
    }

    @Override
    public void notifyInternalEvent(I event, M metadata) {
        mSeenInternal.add(event);
        // Evaluate AND external events
        for (Map.Entry<E, Set<I>> entry : mEventsManagerConfig.getRequireAll().entrySet()) {
            final E external = entry.getKey();
            final Set<I> required = entry.getValue();
            if (required == null) {
                continue;
            }
            if (!mSeenInternal.containsAll(required)) {
                continue;
            }
            if (!prerequisitesSatisfied(external)) {
                continue;
            }
            if (isSuppressed(external)) {
                continue;
            }
            trigger(external, metadata);
        }
        // Evaluate OR external events (requireAny)
        for (Map.Entry<E, Set<I>> entry : mEventsManagerConfig.getRequireAny().entrySet()) {
            final E external = entry.getKey();
            final Set<I> triggers = entry.getValue();

            if (triggers == null || !triggers.contains(event)) {
                continue;
            }
            if (!prerequisitesSatisfied(external)) {
                continue;
            }
            if (isSuppressed(external)) {
                continue;
            }
            trigger(external, metadata);
        }
    }

    private void trigger(E event, M metadata) {
        List<EventHandler> handlersSnapshot;
        synchronized (this) {
            Integer remainingExecs = mRemainingExecutions.get(event);
            if (remainingExecs == null) {
                synchronized (mRemainingExecutions) {
                    remainingExecs = mRemainingExecutions.get(event);
                    if (remainingExecs == null) {
                        remainingExecs = maxExecutions(event);
                        mRemainingExecutions.put(event, remainingExecs);
                    }
                }
            }
            if (remainingExecs == 0) {
                return;
            }
            if (remainingExecs > 0) {
                mRemainingExecutions.put(event, remainingExecs - 1);
            }
            mFired.add(event);
            List<EventHandler> handlers = mSubscriptions.get(event);
            if (handlers == null || handlers.isEmpty()) {
                handlersSnapshot = Collections.emptyList();
            } else {
                handlersSnapshot = new ArrayList<EventHandler<E, M>>(handlers);
            }
        }
        for (EventHandler<E, M> handler : handlersSnapshot) {
            mDelivery.deliver(handler, event, metadata);
        }
    }

    private int maxExecutions(E event) {
        Integer maxExecutions = mEventsManagerConfig.getExecutionLimits().get(event);
        if (maxExecutions != null) {
            return maxExecutions;
        }
        return -1;
    }

    private boolean prerequisitesSatisfied(E external) {
        if (mEventsManagerConfig == null || mEventsManagerConfig.getPrerequisites().isEmpty()) {
            return true;
        }
        final Set<E> prerequisite = mEventsManagerConfig.getPrerequisites().get(external);

        if (prerequisite != null && !prerequisite.isEmpty()) {
            for (E e : prerequisite) {
                if (!eventAlreadyTriggered(e)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isSuppressed(E external) {
        final Set<E> set = mEventsManagerConfig.getSuppressedBy().get(external);
        if (set == null || set.isEmpty()) {
            return false;
        }
        for (E e : set) {
            if (eventAlreadyTriggered(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean eventAlreadyTriggered(E event) {
        return mFired.contains(event);
    }

    @Override
    public void start() {
        mRunning.compareAndSet(false, true);
    }

    @Override
    public void stop() {
        mRunning.compareAndSet(true, false);
    }
}
