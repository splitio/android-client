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

        // The sorted order guarantees that prerequisites and suppressors are evaluated
        // before their dependents.
        for (E externalEvent : mConfig.getEvaluationOrder()) {
            // Check if internal trigger conditions are met (RequireAll or RequireAny)
            boolean internalConditionsMet = checkInternalTriggerConditions(externalEvent, currentSeenInternal, event);
            
            if (!internalConditionsMet) {
                continue;
            }

            // Check external guards (prerequisites and suppression) and fire if all conditions met
            triggerIfConditionsMet(externalEvent, metadata);
        }
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
     * Checks if the internal trigger conditions are met for an external event.
     * Returns true if either RequireAll or RequireAny conditions are satisfied.
     *
     * @param externalEvent the external event to check
     * @param seenInternal all internal events seen so far
     * @param currentEvent the internal event that just arrived
     */
    private boolean checkInternalTriggerConditions(E externalEvent, Set<I> seenInternal, I currentEvent) {
        Set<I> requireAll = mConfig.getRequireAll().get(externalEvent);
        if (requireAll != null && !requireAll.isEmpty() && seenInternal.containsAll(requireAll)) {
            return true;
        }

        // Check RequireAny: The CURRENT internal event must be in one of the groups,
        // and all events in that group must have been seen.
        Set<Set<I>> requireAnyGroups = mConfig.getRequireAny().get(externalEvent);
        if (requireAnyGroups != null && !requireAnyGroups.isEmpty()) {
            for (Set<I> group : requireAnyGroups) {
                // Only consider groups that contain the current event
                if (!group.isEmpty() && group.contains(currentEvent) && seenInternal.containsAll(group)) {
                    return true;
                }
            }
        }

        return false;
    }

}
