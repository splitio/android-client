package io.harness.events;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Core implementation of EventsManager.
 *
 * @param <E> external events type
 * @param <I> internal events type
 * @param <M> metadata type
 */
class EventsManagerCore<E, I, M> implements EventsManager<E, I, M> {

    private static final int UNLIMITED = -1;

    private final Map<E, Set<EventHandler<E, M>>> mSubscriptions = new HashMap<>();
    private final Map<E, Integer> mTriggerCount = new HashMap<>();
    private final Set<I> mSeenInternal = new HashSet<>();

    @NotNull
    private final EventsManagerConfig<E, I> mConfig;
    @NotNull
    private final EventDelivery<E, M> mDelivery;

    @NotNull
    private final ExecutorService mProcessQueue;

    private final Object mLock = new Object();
    private volatile boolean mRunning = true;

    public EventsManagerCore(EventsManagerConfig<E, I> config, EventDelivery<E, M> delivery) {
        mConfig = config == null ? EventsManagerConfig.empty() : config;
        mDelivery = delivery == null ? (h, e, m) -> {} : delivery;
        mProcessQueue = Executors.newSingleThreadExecutor();
    }

    @Override
    public void register(E event, EventHandler<E, M> handler) {
        boolean shouldReplay;
        synchronized (mLock) {
            if (!mRunning) {
                return;
            }

            int max = maxExecutions(event);
            Integer triggered = mTriggerCount.get(event);

            // Replay if limit was reached (event finished all its executions)
            shouldReplay = max != UNLIMITED && triggered != null && triggered >= max;

            if (!shouldReplay) {
                Set<EventHandler<E, M>> handlers = mSubscriptions.get(event);
                if (handlers == null) {
                    handlers = new HashSet<>();
                    mSubscriptions.put(event, handlers);
                }
                handlers.add(handler);
            }
        }

        // Replay if the limit has been reached. Don't add to subscriptions since
        // it will not be triggered again (max executions reached).
        if (shouldReplay) {
            mDelivery.deliver(handler, event, null);
        }
    }

    @Override
    public void unregister(E event) {
        synchronized (mLock) {
            Set<EventHandler<E, M>> handlers = mSubscriptions.get(event);
            if (handlers != null) {
                handlers.clear();
            }
        }
    }

    @Override
    public void notifyInternalEvent(I event, M metadata) {
        if (!mRunning) {
            return;
        }
        try {
            mProcessQueue.execute(() -> processInternal(event, metadata));
        } catch (RejectedExecutionException e) {
            // ignore
        }
    }

    @Override
    public boolean eventAlreadyTriggered(E event) {
        // Wait for pending processing to complete for a consistent view
        CountDownLatch latch = new CountDownLatch(1);
        try {
            mProcessQueue.execute(latch::countDown);
            latch.await();
        } catch (RejectedExecutionException e) {
            // Executor is shut down
        } catch (InterruptedException e) {
            // Restore interrupt status; check current state
            Thread.currentThread().interrupt();
        }

        synchronized (mLock) {
            Integer count = mTriggerCount.get(event);
            if (count == null) {
                return false;
            }

            // For unlimited events, return false since they can always fire again
            int max = maxExecutions(event);
            if (max == UNLIMITED) {
                return false;
            }

            // For limited events, return true only if all executions are done
            return count >= max;
        }
    }

    @Override
    public void destroy() {
        synchronized (mLock) {
            mRunning = false;
            mSubscriptions.clear();
            mTriggerCount.clear();
            mSeenInternal.clear();
        }
        mProcessQueue.shutdown();
    }

    private void processInternal(I event, M metadata) {
        Set<I> currentSeenInternal;
        synchronized (mLock) {
            if (!mRunning) {
                return;
            }
            mSeenInternal.add(event);
            currentSeenInternal = new HashSet<>(mSeenInternal);
        }

        // Track events fired in this processing cycle to avoid re-firing.
        // This prevents infinite loops with unlimited events.
        Set<E> firedInThisCycle = new HashSet<>();

        // Loop until no more events fire in an iteration.
        // Without this loop, events would be missed if their external prerequisites
        // aren't satisfied on the first pass, but become satisfied after other events fire.
        boolean anyEventFiredThisIteration;
        do {
            boolean requireAllEventsFired = evaluateRequireAllEvents(currentSeenInternal, firedInThisCycle, metadata);
            boolean requireAnyEventsFired = evaluateRequireAnyEvents(currentSeenInternal, firedInThisCycle, metadata);
            anyEventFiredThisIteration = requireAllEventsFired || requireAnyEventsFired;

        } while (anyEventFiredThisIteration);
    }

    /**
     * Triggers an external event if all conditions are met.
     * @return true if the event was triggered, false otherwise
     */
    private boolean triggerIfConditionsMet(E event, M metadata) {
        if (!canEventBeTriggered(event)) {
            return false;
        }
        return trigger(event, metadata);
    }

    private boolean canEventBeTriggered(E event) {
        return prerequisitesSatisfied(event) && !isSuppressed(event);
    }

    /**
     * Triggers an external event.
     * @return true if the event was triggered, false if it was already at max executions
     */
    private boolean trigger(E event, M metadata) {
        Set<EventHandler<E, M>> handlersSnapshot = Collections.emptySet();

        synchronized (mLock) {
            int max = maxExecutions(event);
            Integer count = mTriggerCount.get(event);
            int triggered = count != null ? count : 0;

            if (max != UNLIMITED && triggered >= max) {
                return false;
            }

            mTriggerCount.put(event, triggered + 1);

            Set<EventHandler<E, M>> handlers = mSubscriptions.get(event);
            if (handlers != null) {
                handlersSnapshot = new HashSet<>(handlers);
            }
        }

        for (EventHandler<E, M> handler : handlersSnapshot) {
            mDelivery.deliver(handler, event, metadata);
        }
        return true;
    }

    private int maxExecutions(E event) {
        Integer limit = mConfig.getExecutionLimits().get(event);
        return limit != null ? limit : UNLIMITED;
    }

    private boolean prerequisitesSatisfied(E external) {
        Set<E> prerequisites = mConfig.getPrerequisites().get(external);
        if (prerequisites == null || prerequisites.isEmpty()) {
            return true;
        }

        synchronized (mLock) {
            return mTriggerCount.keySet().containsAll(prerequisites);
        }
    }

    private boolean isSuppressed(E external) {
        Set<E> suppressors = mConfig.getSuppressedBy().get(external);
        if (suppressors == null || suppressors.isEmpty()) {
            return false;
        }

        synchronized (mLock) {
            for (E suppressor : suppressors) {
                if (mTriggerCount.containsKey(suppressor)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Evaluates events with AND logic: fire if ALL required internal events have been seen.
     */
    private boolean evaluateRequireAllEvents(Set<I> seenInternal, Set<E> firedInThisCycle, M metadata) {
        boolean anyEventFired = false;
        for (Map.Entry<E, Set<I>> entry : mConfig.getRequireAll().entrySet()) {
            E externalEvent = entry.getKey();
            if (hasAlreadyFiredInCycle(externalEvent, firedInThisCycle)) {
                continue;
            }
            Set<I> requiredInternals = entry.getValue();

            if (allInternalEventsSeen(requiredInternals, seenInternal) && triggerIfConditionsMet(externalEvent, metadata)) {
                firedInThisCycle.add(externalEvent);
                anyEventFired = true;
            }
        }
        return anyEventFired;
    }

    /**
     * Evaluates events with OR-of-ANDs logic: fire if ANY group has ALL its internal events seen.
     */
    private boolean evaluateRequireAnyEvents(Set<I> seenInternal, Set<E> firedInThisCycle, M metadata) {
        boolean anyEventFired = false;
        for (Map.Entry<E, Set<Set<I>>> entry : mConfig.getRequireAny().entrySet()) {
            E externalEvent = entry.getKey();
            if (hasAlreadyFiredInCycle(externalEvent, firedInThisCycle)) {
                continue;
            }
            Set<Set<I>> requiredGroups = entry.getValue();

            if (anyGroupSatisfied(requiredGroups, seenInternal) && triggerIfConditionsMet(externalEvent, metadata)) {
                firedInThisCycle.add(externalEvent);
                anyEventFired = true;
            }
        }
        return anyEventFired;
    }

    private boolean hasAlreadyFiredInCycle(E event, Set<E> firedInThisCycle) {
        return firedInThisCycle.contains(event);
    }

    private boolean allInternalEventsSeen(Set<I> requiredInternals, Set<I> seenInternal) {
        return !requiredInternals.isEmpty() && seenInternal.containsAll(requiredInternals);
    }

    private boolean anyGroupSatisfied(Set<Set<I>> requiredGroups, Set<I> seenInternal) {
        for (Set<I> group : requiredGroups) {
            if (allInternalEventsSeen(group, seenInternal)) {
                return true;
            }
        }
        return false;
    }
}
