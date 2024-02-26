package io.split.android.client.events.executors;


import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.service.executor.SplitTaskExecutor;

/**
 * Created by sarrubia on 4/6/18.
 */

public class SplitEventExecutorFactory {

    public static SplitEventExecutor factory(SplitTaskExecutor taskExecutor, SplitEvent event, SplitEventTask task, SplitEventExecutorResources resources) {

        SplitEventExecutor executor;

        switch (event) {
            case SDK_READY:
            case SDK_READY_FROM_CACHE:
            case SDK_UPDATE:
            case SDK_READY_TIMED_OUT:
                executor = new SplitEventExecutorWithClient(taskExecutor, task, resources.getSplitClient(), event);
                break;

            default:
                throw new IllegalArgumentException();
        }

        return executor;
    }
}
