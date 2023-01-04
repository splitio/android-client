package io.split.android.client.events.executors;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;

public class MainThreadSplitTask extends ClientEventSplitTask {

    MainThreadSplitTask(SplitEventTask task, SplitClient client) {
        super(task, client);
    }

    @Override
    public void action() {
        mTask.onPostExecutionView(mSplitClient);
    }
}
