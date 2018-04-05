package io.split.android.client.events.executors;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.utils.Logger;
import io.split.android.engine.SDKReadinessGates;

/**
 * Created by sarrubia on 4/5/18.
 */

public class SplitEventExecutorOnReadyTimeOut extends SplitEventExecutorBase {

    private final long _millisecondsTimeout;
    private final SDKReadinessGates _gates;

    public SplitEventExecutorOnReadyTimeOut(long timeout, SDKReadinessGates gates, SplitEventTask task) {

        super(task);

        _millisecondsTimeout = timeout;
        _gates = gates;
    }

    @Override
    protected boolean shouldCallOnPostExecutionMethod(SplitClient client) {
        try {
            return !_gates.isSDKReady(_millisecondsTimeout); //SDK is not ready before specified time
        } catch (InterruptedException e) {
            Logger.e("Interrupted Exception on SplitEventExecutorOnReadyTimeOut", e);
            return true; //SDK is not ready or load has been interrupted
        }
    }
}