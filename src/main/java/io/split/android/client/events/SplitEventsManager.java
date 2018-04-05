package io.split.android.client.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.events.executors.SplitEventExecutorAbstract;
import io.split.android.client.events.executors.SplitEventExecutorOnReady;
import io.split.android.client.events.executors.SplitEventExecutorOnReadyTimeOut;
import io.split.android.engine.SDKReadinessGates;

/**
 * Created by sarrubia on 4/3/18.
 */

public class SplitEventsManager {

    //https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ArrayBlockingQueue.html

    private final SplitClientConfig _config;
    private SDKReadinessGates _gates;
    private SplitClient _client;

    private ArrayBlockingQueue<SplitEvent> _queue;

    private Map<SplitEvent, List<SplitEventTask>> _suscriptions;

    public SplitEventsManager(SplitClient client, SplitClientConfig config, SDKReadinessGates gates){
        _client = client;
        _config = config;
        _gates = gates;

        _queue = new ArrayBlockingQueue<>(10);
        _suscriptions = new HashMap<>();
    }

    public void register(SplitEvent event, SplitEventTask task){

        if (!_suscriptions.containsKey(event)) {
            _suscriptions.put(event, new ArrayList<>());
        }
        _suscriptions.get(event).add(task);

        /*
        SplitEventExecutorAbstract executor = null;

        switch(event){
            case SDK_READY:
                executor = new SplitEventExecutorOnReady(_gates, task);
                break;

            case SDK_READY_TIMED_OUT:
                executor = new SplitEventExecutorOnReadyTimeOut(_config.blockUntilReady(), _gates, task);
                break;
        }

        if (executor != null){
            executor.execute(_client);
        }
        * */
    }

    public void run(){

    }
}
