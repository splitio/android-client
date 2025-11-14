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

    private final Map<E, List<io.harness.events.EventHandler>> mSubscriptions = new ConcurrentHashMap<>();
    private final Map<E, Integer> mRemainingExecutions = new ConcurrentHashMap<>();
    protected final Set<E> mFired = Collections.newSetFromMap(new ConcurrentHashMap<E, Boolean>());
    private final Set<I> mSeenInternal = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<E, M> mLastMetadata = new ConcurrentHashMap<>();

    private final TriggerDependencies<E, I> mTriggerDependencies;
    @NonNull
    private final EventDelivery<E, M> mDelivery;
    private final AtomicBoolean mRunning = new AtomicBoolean(false);

    public EventsManagerCore(@Nullable TriggerDependencies<E, I> triggerDependencies, EventDelivery<E, M> delivery) {
        mTriggerDependencies = triggerDependencies == null ? TriggerDependencies.empty() : triggerDependencies;
        mDelivery = delivery;
    }

    @Override
    public void register(E event, io.harness.events.EventHandler handler) {
        // If the event was already triggered, we replay it
        if (eventAlreadyTriggered(event)) {
            mDelivery.deliver(handler, event, null);
            return;
        }

        // Add new handler to the corresponding event's handlers
        List<io.harness.events.EventHandler> list = mSubscriptions.get(event);
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
        for (Map.Entry<E, Set<I>> entry : mTriggerDependencies.getRequireAll().entrySet()) {
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
        for (Map.Entry<E, Set<I>> entry : mTriggerDependencies.getRequireAny().entrySet()) {
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
        List<io.harness.events.EventHandler> handlersSnapshot;
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
            List<io.harness.events.EventHandler> handlers = mSubscriptions.get(event);
            if (handlers == null || handlers.isEmpty()) {
                handlersSnapshot = Collections.emptyList();
            } else {
                handlersSnapshot = new ArrayList<EventHandler<E, M>>(handlers);
            }
        }
        for (io.harness.events.EventHandler handler : handlersSnapshot) {
            mDelivery.deliver(handler, event, metadata);
        }
    }

    private int maxExecutions(E event) {
        Integer maxExecutions = mTriggerDependencies.getExecutionLimits().get(event);
        if (maxExecutions != null) {
            return maxExecutions;
        }
        return -1;
    }

    private boolean prerequisitesSatisfied(E external) {
        if (mTriggerDependencies == null || mTriggerDependencies.getPrerequisites().isEmpty()) {
            return true;
        }
        final Set<E> prerequisite = mTriggerDependencies.getPrerequisites().get(external);

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
        final Set<E> set = mTriggerDependencies.getSuppressedBy().get(external);
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
