package io.split.android.client.events.executors;

import android.os.AsyncTask;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;

/**
 * Created by sarrubia on 4/3/18.
 */

public abstract class SplitEventExecutorAbstract {

    protected AsyncTask<SplitClient, Void, SplitClient> _asyncTansk;

    protected SplitEventTask _task;

    protected SplitEventExecutorAbstract(SplitEventTask task){
        _task = task;
    }

    public abstract void execute();
}
