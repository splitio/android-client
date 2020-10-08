package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.BroadcastedEventListener;
import io.split.android.client.service.sseclient.feedbackchannel.PushStatusEvent;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;
import io.split.android.client.service.sseclient.sseclient.BackoffCounterTimer;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncManagerImpl implements SyncManager, BroadcastedEventListener {

    private final SplitClientConfig mSplitClientConfig;
    private final PushManagerEventBroadcaster mPushManagerEventBroadcaster;
    private final Synchronizer mSynchronizer;
    private final PushNotificationManager mPushNotificationManager;
    private SplitUpdatesWorker mSplitUpdateWorker;
    private MySegmentsUpdateWorker mMySegmentUpdateWorker;
    private BackoffCounterTimer mStreamingReconnectTimer;



    private AtomicBoolean isPollingEnabled;

    public SyncManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                           @NonNull Synchronizer synchronizer,
                           @NonNull PushNotificationManager pushNotificationManager,
                           @NonNull SplitUpdatesWorker splitUpdateWorker,
                           @NonNull MySegmentsUpdateWorker mySegmentUpdateWorker,
                           @NonNull PushManagerEventBroadcaster pushManagerEventBroadcaster,
                           @NonNull BackoffCounterTimer streamingReconnectTimer) {

        mSynchronizer = checkNotNull(synchronizer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mPushNotificationManager = checkNotNull(pushNotificationManager);
        mSplitUpdateWorker = checkNotNull(splitUpdateWorker);
        mMySegmentUpdateWorker = checkNotNull(mySegmentUpdateWorker);
        mPushManagerEventBroadcaster = checkNotNull(pushManagerEventBroadcaster);
        mStreamingReconnectTimer = checkNotNull(streamingReconnectTimer);

        isPollingEnabled = new AtomicBoolean(false);
    }


    @Override
    public void start() {

        mSynchronizer.loadAndSynchronizeSplits();
        mSynchronizer.loadMySegmentsFromCache();
        mSynchronizer.synchronizeMySegments();
        isPollingEnabled.set(!mSplitClientConfig.streamingEnabled());
        if (mSplitClientConfig.streamingEnabled()) {
            mPushManagerEventBroadcaster.register(this);
            mSplitUpdateWorker.start();
            mMySegmentUpdateWorker.start();
            mPushNotificationManager.start();

        } else {
            mSynchronizer.startPeriodicFetching();
        }
        mSynchronizer.startPeriodicRecording();
    }

    @Override
    public void pause() {
        mSynchronizer.pause();
        if(mSplitClientConfig.streamingEnabled()) {
            mPushNotificationManager.pause();
        }
    }

    @Override
    public void resume() {
        mSynchronizer.resume();
        if(mSplitClientConfig.streamingEnabled()) {
            mPushNotificationManager.resume();
        }
    }

    @Override
    public void flush() {
        mSynchronizer.flush();
    }

    @Override
    public void pushEvent(Event event) {
        mSynchronizer.pushEvent(event);
    }

    @Override
    public void pushImpression(Impression impression) {
        mSynchronizer.pushImpression(impression);
    }

    @Override
    public void stop() {
        mSynchronizer.stopPeriodicFetching();
        mSynchronizer.stopPeriodicRecording();
        mSynchronizer.destroy();
        mPushNotificationManager.stop();
        mSplitUpdateWorker.stop();
        mMySegmentUpdateWorker.stop();
    }

    @Override
    public void onEvent(PushStatusEvent message) {
        switch (message.getMessage()) {
            case PUSH_SUBSYSTEM_UP:
                Logger.d("Push Subsystem Up event message received.");
                mSynchronizer.synchronizeSplits();
                mSynchronizer.synchronizeMySegments();
                mSynchronizer.stopPeriodicFetching();
                mStreamingReconnectTimer.cancel();
                isPollingEnabled.set(false);
                break;

            case PUSH_SUBSYSTEM_DOWN:
                Logger.d("Push Subsystem Down event message received.");
                enablePolling();
                mStreamingReconnectTimer.cancel();
                break;

            case PUSH_RETRYABLE_ERROR:
                Logger.d("Push Subsystem recoverable error received.");
                enablePolling();
                mPushNotificationManager.stop();
                mStreamingReconnectTimer.schedule();
                break;

            case PUSH_NON_RETRYABLE_ERROR:
                Logger.d("Push Subsystem non recoverable error received.");
                enablePolling();
                mPushNotificationManager.stop();
                mStreamingReconnectTimer.cancel();
                break;

            case PUSH_DISABLED:
                Logger.d("Push Subsystem Down event message received.");
                enablePolling();
                mStreamingReconnectTimer.cancel();
                mPushNotificationManager.stop();
                break;

            default:
                Logger.e("Invalid SSE event received: " + message.getMessage());
        }
    }

    private void enablePolling() {
        if (!isPollingEnabled.get()) {
            isPollingEnabled.set(true);
            mSynchronizer.startPeriodicFetching();
            Logger.i("Polling enabled.");
        }
    }
}
