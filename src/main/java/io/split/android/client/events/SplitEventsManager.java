package io.split.android.client.events;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.executors.SplitEventExecutorAbstract;
import io.split.android.client.events.executors.SplitEventExecutorFactory;
import io.split.android.client.events.executors.SplitEventExecutorResources;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by sarrubia on 4/3/18.
 */

public class SplitEventsManager implements ISplitEventsManager, Runnable {

    private final static int QUEUE_CAPACITY = 10;

    private SplitClientConfig _config;

    // TODO: Analyze removing this annotation
    @SuppressWarnings("FieldCanBeLocal")
    private final ScheduledExecutorService _scheduler;

    private ArrayBlockingQueue<SplitInternalEvent> _queue;

    private Map<SplitEvent, List<SplitEventTask>> _suscriptions;

    private SplitEventExecutorResources _resources;

    private Map<SplitEvent, Integer> _executionTimes;

    private Set<SplitInternalEvent> _triggered;

    public SplitEventsManager(SplitClientConfig config) {

        _config = config;

        _queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        _suscriptions = new ConcurrentHashMap<>();

        _executionTimes = new ConcurrentHashMap<>();
        _resources = new SplitEventExecutorResources();
        _triggered = new ConcurrentHashMap<SplitInternalEvent, Integer>().keySet();

        registerMaxAllowebExecutionTimesPerEvent();

        Runnable SDKReadyTimeout = new Runnable() {
            @Override
            public void run() {
                try {
                    if (_config.blockUntilReady() > 0) {
                        Thread.sleep(_config.blockUntilReady());
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

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Split-EventsManager-%d")
                .build();
        _scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        _scheduler.submit(this);

    }


    /**
     * This method should registering the allowed maximum times of event trigger
     * EXAMPLE: SDK_READY should be triggered only once
     */
    private void registerMaxAllowebExecutionTimesPerEvent() {
        _executionTimes.put(SplitEvent.SDK_READY, 1);
        _executionTimes.put(SplitEvent.SDK_READY_TIMED_OUT, 1);
        _executionTimes.put(SplitEvent.SDK_READY_FROM_CACHE, 1);
        _executionTimes.put(SplitEvent.SDK_UPDATED, -1);
    }

    public SplitEventExecutorResources getExecutorResources() {
        return _resources;
    }

    public void notifyInternalEvent(SplitInternalEvent internalEvent) {

        checkNotNull(internalEvent);

        try {
            _queue.add(internalEvent);
        } catch (IllegalStateException e) {
            Logger.d("Internal events queue is full");
        }
    }

    public void register(SplitEvent event, SplitEventTask task) {

        checkNotNull(event);
        checkNotNull(task);

        // If event is already triggered, execute the task
        if (_executionTimes.containsKey(event) && _executionTimes.get(event) == 0) {
            executeTask(event, task);
            return;
        }

        if (!_suscriptions.containsKey(event)) {
            _suscriptions.put(event, new ArrayList<>());
        }
        _suscriptions.get(event).add(task);
    }

    public boolean eventAlreadyTriggered(SplitEvent event) {
        return _executionTimes.get(event) == 0;
    }

    @Override
    public void run() {
        // This code was intentionally designed this way
        // TODO: Analize refactor
        //noinspection InfiniteLoopStatement,InfiniteLoopStatement
        while (true) {
            triggerEventsWhenAreAvailable();
        }
    }

    private void triggerEventsWhenAreAvailable() {
        try {
            SplitInternalEvent event = _queue.take(); //Blocking method (waiting if necessary until an element becomes available.)
            _triggered.add(event);
            switch (event) {
                case SPLITS_UPDATED:
                case MY_SEGEMENTS_UPDATED:
                    if (isTriggered(SplitEvent.SDK_READY)) {
                        trigger(SplitEvent.SDK_UPDATED);
                        return;
                    }
                    triggerSdkReadyIfNeeded();
                    break;

                case SPLITS_LOADED_FROM_STORAGE:
                case MYSEGMENTS_LOADED_FROM_STORAGE:
                    if (isTriggered(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE) &&
                            isTriggered(SplitInternalEvent.MYSEGMENTS_LOADED_FROM_STORAGE)) {
                        trigger(SplitEvent.SDK_READY_FROM_CACHE);
                    }
                    break;

                case SDK_READY_TIMEOUT_REACHED:
                    if (isTriggered(SplitEvent.SDK_READY)) {
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
        Integer times = _executionTimes.get(event);
        return times != null ? times > 0 : false;
    }

    private void triggerSdkReadyIfNeeded() {
        if (isTriggered(SplitInternalEvent.MY_SEGEMENTS_UPDATED) &&
        isTriggered(SplitInternalEvent.SPLITS_UPDATED) &&
        !isTriggered(SplitEvent.SDK_READY)){
            trigger(SplitEvent.SDK_READY);
        }
    }

    private void trigger(SplitEvent event) {

        // If executionTimes is zero, maximum executions has been reached
        if (_executionTimes.get(event) == 0) {
            return;
            // If executionTimes is grater than zero, maximum executions decrease 1
        } else if (_executionTimes.get(event) > 0) {
            _executionTimes.put(event, _executionTimes.get(event) - 1);
        } //If executionTimes is lower than zero, execute it without limitation

        if (_suscriptions.containsKey(event)) {
            List<SplitEventTask> toExecute = _suscriptions.get(event);

            for (SplitEventTask task : toExecute) {
                executeTask(event, task);
            }
        }
    }

    private void executeTask(SplitEvent event, SplitEventTask task) {
        SplitEventExecutorAbstract executor = SplitEventExecutorFactory.factory(event, task, _resources);
        if (executor != null) {
            executor.execute();
        }
    }

    private boolean isTriggered(SplitInternalEvent event) {
        return _triggered.contains(event);
    }
}
