package io.split.android.client.service.sseclient.sseclient;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent.EventType;
import io.split.android.client.service.sseclient.spi.StreamingScheduler;
import io.split.android.client.utils.logger.Logger;

public class SseRefreshTokenTimer {
    private final static int RECONNECT_TIME_BEFORE_TOKEN_EXP_IN_SECONDS = 600;
    private final StreamingScheduler mScheduler;
    private final PushManagerEventBroadcaster mBroadcasterChannel;
    private String mTaskId;

    public SseRefreshTokenTimer(@NonNull StreamingScheduler scheduler, @NonNull PushManagerEventBroadcaster broadcasterChannel) {
        mScheduler = checkNotNull(scheduler);
        mBroadcasterChannel = checkNotNull(broadcasterChannel);
    }

    public void cancel() {
        mScheduler.cancel(mTaskId);
    }

    public void schedule(long issueAtTime, long expirationTime) {
        cancel();
        long reconnectTime = reconnectTime(issueAtTime, expirationTime);
        mTaskId = mScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Logger.d("Informing sse token expired through pushing retryable error.");
                mBroadcasterChannel.pushMessage(new PushStatusEvent(EventType.PUSH_RETRYABLE_ERROR));
            }
        }, reconnectTime, new StreamingScheduler.TaskExecutionListener() {
            @Override
            public void onTaskExecuted() {
                mTaskId = null;
            }
        });
    }

    private long reconnectTime(long issuedAtTime, long expirationTime) {
        return Math.max((expirationTime - issuedAtTime) - RECONNECT_TIME_BEFORE_TOKEN_EXP_IN_SECONDS
                , 0L);
    }

}
