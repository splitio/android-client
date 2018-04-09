package io.split.android.client.events;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.executors.SplitEventExecutorAbstract;
import io.split.android.client.events.executors.SplitEventExecutorFactory;
import io.split.android.client.events.executors.SplitEventExecutorResources;
import io.split.android.client.utils.Logger;

/**
 * Created by sarrubia on 4/3/18.
 */

public class SplitEventsManager implements Runnable {

    private final int QUEUE_CAPACITY = 10;

    private SplitClientConfig _config;

    private final ScheduledExecutorService _scheduler;

    private ArrayBlockingQueue<SplitInternalEvent> _queue;

    private Map<SplitEvent, List<SplitEventTask>> _suscriptions;



    private SplitEventExecutorResources _resources;

    private boolean _eventMySegmentsAreReady = false;
    private boolean _eventSplitsAreReady = false;

    private Map<SplitEvent, Integer> _executionTimes;

    public SplitEventsManager(SplitClientConfig config){

        _config = config;

        _queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        _suscriptions = new HashMap<>();

        _executionTimes = new HashMap<>();
        _resources = new SplitEventExecutorResources();

        registerMaxAllowebExecutionTimesPerEvent();

        Runnable SDKReadyTimeout = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(_config.blockUntilReady());
                    notifyInternalEvent(SplitInternalEvent.SDK_READY_TIMEOUT_REACHED);
                } catch (InterruptedException e) {
                    Logger.d(e.getMessage());
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


    private void registerMaxAllowebExecutionTimesPerEvent(){
        _executionTimes.put(SplitEvent.SDK_READY, 1);
        _executionTimes.put(SplitEvent.SDK_READY_TIMED_OUT, 1);
    }

    public SplitEventExecutorResources getExecutorResources(){
        return _resources;
    }

    public void notifyInternalEvent(SplitInternalEvent internalEvent){
        try{
            _queue.add(internalEvent);
        } catch (IllegalStateException e) {
            Logger.d("Internal events queue is full");
        }
    }

    public void register(SplitEvent event, SplitEventTask task){

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


    @Override
    public void run(){
        while(true){
            triggerEventsWhenAreAvailable();
        }
    }

    private void triggerEventsWhenAreAvailable(){
        try {
            SplitInternalEvent event = _queue.take(); //Blocking method (waiting if necessary until an element becomes available.)
            switch (event){
                case SPLITS_ARE_READY:
                    _eventSplitsAreReady = true;
                    if (_eventMySegmentsAreReady) {
                        trigger(SplitEvent.SDK_READY);
                    }
                    break;
                case MYSEGEMENTS_ARE_READY:
                    _eventMySegmentsAreReady = true;
                    if (_eventSplitsAreReady) {
                        trigger(SplitEvent.SDK_READY);
                    }
                    break;
                case SDK_READY_TIMEOUT_REACHED:
                    if (!_eventSplitsAreReady || !_eventMySegmentsAreReady ) {
                        trigger(SplitEvent.SDK_READY_TIMED_OUT);
                    }
                    break;
            }
        } catch (InterruptedException e) {
            Logger.d(e.getMessage());
        }
    }

    private void trigger(SplitEvent event) {

        // If executionTimes is zero, maximum executions has been reached
        if (_executionTimes.get(event) == 0){
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

    private void executeTask(SplitEvent event, SplitEventTask task){
        SplitEventExecutorAbstract executor = SplitEventExecutorFactory.factory(event, task, _resources);
        if (executor != null){
            executor.execute();
        }
    }
}
