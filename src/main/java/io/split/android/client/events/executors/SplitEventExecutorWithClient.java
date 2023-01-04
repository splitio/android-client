package io.split.android.client.events.executors;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutor;

public class SplitEventExecutorWithClient implements SplitEventExecutorAbstract {

    private final SplitTaskExecutor mSplitTaskExecutor;
    private final SplitTask mBackgroundSplitTask;
    private final SplitTask mMainThreadSplitTask;

    public SplitEventExecutorWithClient(@NonNull SplitTaskExecutor taskExecutor,
                                        @NonNull SplitEventTask task,
                                        @NonNull SplitClient client) {
        mSplitTaskExecutor = checkNotNull(taskExecutor);
        mBackgroundSplitTask = new ClientEventSplitTask(task, client, false);
        mMainThreadSplitTask = new ClientEventSplitTask(task, client, true);
    }

    public void execute() {
        mSplitTaskExecutor.submit(mBackgroundSplitTask, null);
        mSplitTaskExecutor.submitOnMainThread(mMainThreadSplitTask);
    }
}
