package io.split.android.client.events.executors;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.engine.SDKReadinessGates;

/**
 * Created by sarrubia on 4/3/18.
 */

public class SplitEventExecutorOnReady extends SplitEventExecutorBase {

    private final SDKReadinessGates _gates;

    public SplitEventExecutorOnReady(SDKReadinessGates gates, SplitEventTask task) {

        super(task);
        _gates = gates;
    }

    @Override
    protected boolean shouldCallOnPostExecutionMethod(SplitClient client) {
        return _gates.awaitSDKReady();
    }
}
