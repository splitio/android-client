package io.split.android.client.service.sseclient.sseclient;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;
import static java.lang.reflect.Modifier.PRIVATE;

public class SseDisconnectionTimer implements SplitTaskExecutionListener {

    private final static int DISCONNECT_ON_BG_TIME_IN_SECONDS = 60;
    private final SplitTaskExecutor mTaskExecutor;
    private final int mInitialDelayInSeconds;
    private String mTaskId;

    public SseDisconnectionTimer(@NonNull SplitTaskExecutor taskExecutor) {
        this(taskExecutor, DISCONNECT_ON_BG_TIME_IN_SECONDS);
    }

    @VisibleForTesting
    public SseDisconnectionTimer(@NonNull SplitTaskExecutor taskExecutor, int initialDelayInSeconds) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mInitialDelayInSeconds = initialDelayInSeconds;
    }

    public void cancel() {
        mTaskExecutor.stopTask(mTaskId);
    }

    public void schedule(SplitTask task) {
        cancel();
        mTaskId = mTaskExecutor.schedule(task, mInitialDelayInSeconds, this);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        mTaskId = null;
    }
}
