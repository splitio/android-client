package io.split.android.client.events.executors;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClient;
import io.split.android.client.TimeChecker;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutor;

public class SplitEventExecutorWithClient implements SplitEventExecutor {

    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTask mBackgroundSplitTask;
    private final SplitTask mMainThreadSplitTask;
    private final SplitEvent mEvent;

    public SplitEventExecutorWithClient(@NonNull SplitTaskExecutor taskExecutor,
                                        @NonNull SplitEventTask task,
                                        @NonNull SplitClient client,
                                        @NonNull SplitEvent event) {
        mSplitTaskExecutor = checkNotNull(taskExecutor);
        mBackgroundSplitTask = new ClientEventSplitTask(task, client, false);
        mMainThreadSplitTask = new ClientEventSplitTask(task, client, true);
        mEvent = checkNotNull(event);
    }

    public void execute() {
        TimeChecker.timeSinceStartLog("Running event "+mEvent+" on global at");
        mSplitTaskExecutor.submit(mBackgroundSplitTask, null);
        TimeChecker.timeSinceStartLog("Running event "+mEvent+" on main at");
        mSplitTaskExecutor.submitOnMainThread(mMainThreadSplitTask);
    }
}
