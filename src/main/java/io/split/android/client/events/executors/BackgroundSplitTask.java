package io.split.android.client.events.executors;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;

class BackgroundSplitTask extends ClientEventSplitTask {

    BackgroundSplitTask(SplitEventTask task, SplitClient client) {
        super(task, client);
    }

    @Override
    public void action() {
        mTask.onPostExecution(mSplitClient);
    }
}
