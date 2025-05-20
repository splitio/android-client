package io.split.android.client.service.sseclient.sseclient;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType;
import io.split.android.client.utils.logger.Logger;

public class SseRefreshTokenTimer implements SplitTaskExecutionListener {
    private final static int RECONNECT_TIME_BEFORE_TOKEN_EXP_IN_SECONDS = 600;
    SplitTaskExecutor mTaskExecutor;
    PushManagerEventBroadcaster mBroadcasterChannel;
    String mTaskId;

    public SseRefreshTokenTimer(@NonNull SplitTaskExecutor taskExecutor, @NonNull PushManagerEventBroadcaster broadcasterChannel) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mBroadcasterChannel = checkNotNull(broadcasterChannel);
    }

    public void cancel() {
        mTaskExecutor.stopTask(mTaskId);
    }

    public void schedule(long issueAtTime, long expirationTime) {
        cancel();
        long reconnectTime = reconnectTime(issueAtTime, expirationTime);
        mTaskId = mTaskExecutor.schedule(new SplitTask() {
            @NonNull
            @Override
            public SplitTaskExecutionInfo execute() {
                Logger.d("Informing sse token expired through pushing retryable error.");
                mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_RETRYABLE_ERROR));
                return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
            }
        }, reconnectTime, null);
    }

    private long reconnectTime(long issuedAtTime, long expirationTime) {
        return Math.max((expirationTime - issuedAtTime) - RECONNECT_TIME_BEFORE_TOKEN_EXP_IN_SECONDS
                , 0L);
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        mTaskId = null;
    }
}
