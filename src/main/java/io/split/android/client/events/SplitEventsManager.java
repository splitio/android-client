package io.split.android.client.events;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.executors.SplitEventExecutorAbstract;
import io.split.android.client.events.executors.SplitEventExecutorFactory;
import io.split.android.client.events.executors.SplitEventExecutorResources;
import io.split.android.client.events.executors.SplitEventExecutorResourcesImpl;
import io.split.android.client.utils.Logger;

/**
 * Created by sarrubia on 4/3/18.
 */

public class SplitEventsManager extends BaseEventsManager implements ISplitEventsManager, ListenableEventsManager, Runnable {

    private final Map<SplitEvent, List<SplitEventTask>> mSubscriptions;

    private SplitEventExecutorResources mResources;

    private final Map<SplitEvent, Integer> mExecutionTimes;

    public SplitEventsManager(SplitClientConfig config) {
        super();

        mSubscriptions = new ConcurrentHashMap<>();
        mExecutionTimes = new ConcurrentHashMap<>();
        mResources = new SplitEventExecutorResourcesImpl();
        registerMaxAllowedExecutionTimesPerEvent();

        Runnable SDKReadyTimeout = new Runnable() {
            @Override
            public void run() {
                try {
                    if (config.blockUntilReady() > 0) {
                        Thread.sleep(config.blockUntilReady());
                        notifyInternalEvent(SplitInternalEvent.SDK_READY_TIMEOUT_REACHED);
                    }
                } catch (InterruptedException e) {
                    //InterruptedException could be thrown by Thread.sleep trying to wait before check if sdk is ready
                    Logger.d("Waiting before to check if SDK is READY has been interrupted", e.getMessage());
                    notifyInternalEvent(SplitInternalEvent.SDK_READY_TIMEOUT_REACHED);
                } catch (Throwable e) {
                    Logger.d("Waiting before to check if SDK is READY interrupted ", e.getMessage());
                    notifyInternalEvent(SplitInternalEvent.SDK_READY_TIMEOUT_REACHED);
                }
            }
        };
        new Thread(SDKReadyTimeout).start();
    }

    @VisibleForTesting
    public void setExecutionResources(SplitEventExecutorResources resources) {
        mResources = resources;
    }

    /**
     * This method should register the allowed maximum times of event trigger
     * EXAMPLE: SDK_READY should be triggered only once
     */
    private void registerMaxAllowedExecutionTimesPerEvent() {
        mExecutionTimes.put(SplitEvent.SDK_READY, 1);
        mExecutionTimes.put(SplitEvent.SDK_READY_TIMED_OUT, 1);
        mExecutionTimes.put(SplitEvent.SDK_READY_FROM_CACHE, 1);
        mExecutionTimes.put(SplitEvent.SDK_UPDATE, -1);
    }

    @Override
    public SplitEventExecutorResources getExecutorResources() {
        return mResources;
    }

    @Override
    public void notifyInternalEvent(SplitInternalEvent internalEvent) {
        checkNotNull(internalEvent);
        // Avoid adding to queue for fetched events if sdk is ready
        // These events were added to handle updated event logic in this component
        // and also to fix some issues when processing queue that made sdk update
        // fire on init
        if((internalEvent == SplitInternalEvent.SPLITS_FETCHED
                || internalEvent == SplitInternalEvent.MY_SEGMENTS_FETCHED) &&
                isTriggered(SplitEvent.SDK_READY)) {
            return;
        }
        try {
            mQueue.add(internalEvent);
        } catch (IllegalStateException e) {
            Logger.d("Internal events queue is full");
        }
    }

    public void register(SplitEvent event, SplitEventTask task) {

        checkNotNull(event);
        checkNotNull(task);

        // If event is already triggered, execute the task
        if (mExecutionTimes.containsKey(event) && mExecutionTimes.get(event) == 0) {
            executeTask(event, task);
            return;
        }

        if (!mSubscriptions.containsKey(event)) {
            mSubscriptions.put(event, new ArrayList<>());
        }
        mSubscriptions.get(event).add(task);
    }

    public boolean eventAlreadyTriggered(SplitEvent event) {
        return mExecutionTimes.get(event) == 0;
    }

    private boolean wasTriggered(SplitInternalEvent event) {
        return mTriggered.contains(event);
    }

    @Override
    protected void triggerEventsWhenAreAvailable() {
        try {
            SplitInternalEvent event = mQueue.take(); //Blocking method (waiting if necessary until an element becomes available.)
            mTriggered.add(event);
            switch (event) {
                case SPLITS_UPDATED:
                case MY_SEGMENTS_UPDATED:
                    if (isTriggered(SplitEvent.SDK_READY)) {
                        trigger(SplitEvent.SDK_UPDATE);
                        return;
                    }
                    triggerSdkReadyIfNeeded();
                    break;

                case SPLITS_FETCHED:
                case MY_SEGMENTS_FETCHED:
                    if (isTriggered(SplitEvent.SDK_READY)) {
                        return;
                    }
                    triggerSdkReadyIfNeeded();
                    break;

                case SPLITS_LOADED_FROM_STORAGE:
                case MY_SEGMENTS_LOADED_FROM_STORAGE:
                case ATTRIBUTES_LOADED_FROM_STORAGE:
                    if (wasTriggered(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE) &&
                            wasTriggered(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE) &&
                            wasTriggered(SplitInternalEvent.ATTRIBUTES_LOADED_FROM_STORAGE)) {
                        trigger(SplitEvent.SDK_READY_FROM_CACHE);
                    }
                    break;

                case SPLIT_KILLED_NOTIFICATION:
                    if (isTriggered(SplitEvent.SDK_READY)) {
                        trigger(SplitEvent.SDK_UPDATE);
                    }
                    break;

                case SDK_READY_TIMEOUT_REACHED:
                    if (!isTriggered(SplitEvent.SDK_READY)) {
                        trigger(SplitEvent.SDK_READY_TIMED_OUT);
                    }
                    break;
            }
        } catch (InterruptedException e) {
            //Catching the InterruptedException that can be thrown by _queue.take() if interrupted while waiting
            // for further information read https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ArrayBlockingQueue.html#take()
            Logger.d(e.getMessage());
        }
    }

    // MARK: Helper functions.
    private boolean isTriggered(SplitEvent event) {
        Integer times = mExecutionTimes.get(event);
        return times != null ? times == 0 : false;
    }

    private void triggerSdkReadyIfNeeded() {
        if ((wasTriggered(SplitInternalEvent.MY_SEGMENTS_UPDATED) || wasTriggered(SplitInternalEvent.MY_SEGMENTS_FETCHED)) &&
                (wasTriggered(SplitInternalEvent.SPLITS_UPDATED) || wasTriggered(SplitInternalEvent.SPLITS_FETCHED)) &&
                !isTriggered(SplitEvent.SDK_READY)) {
            trigger(SplitEvent.SDK_READY);
        }
    }

    private void trigger(SplitEvent event) {
        // If executionTimes is zero, maximum executions has been reached
        if (mExecutionTimes.get(event) == 0) {
            return;
            // If executionTimes is grater than zero, maximum executions decrease 1
        } else if (mExecutionTimes.get(event) > 0) {
            mExecutionTimes.put(event, mExecutionTimes.get(event) - 1);
        } //If executionTimes is lower than zero, execute it without limitation
        if (mSubscriptions.containsKey(event)) {
            List<SplitEventTask> toExecute = mSubscriptions.get(event);
            for (SplitEventTask task : toExecute) {
                executeTask(event, task);
            }
        }
    }

    private void executeTask(SplitEvent event, SplitEventTask task) {
        SplitEventExecutorAbstract executor = SplitEventExecutorFactory.factory(event, task, mResources);
        if (executor != null) {
            executor.execute();
        }
    }
}
