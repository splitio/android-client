package io.split.android.client.events.executors;


import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;

/**
 * Created by sarrubia on 4/6/18.
 */

public class SplitEventExecutorFactory {


    public static SplitEventExecutorAbstract factory(SplitEvent event, SplitEventTask task, SplitEventExecutorResources resources){

        SplitEventExecutorAbstract executor = null;

        switch(event){
            case SDK_READY:
                executor = new SplitEventExecutorWithClient(task, resources.getSplitClient());
                break;

            case SDK_READY_TIMED_OUT:
                executor = new SplitEventExecutorWithClient(task, resources.getSplitClient());
                break;
        }

        return executor;
    }
}
