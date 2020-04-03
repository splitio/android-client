package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.feedbackchannel.PushManagerEventBroadcaster;
import io.split.android.client.service.sseclient.feedbackchannel.BroadcastedEventListener;
import io.split.android.client.service.sseclient.feedbackchannel.BroadcastedEvent;
import io.split.android.client.service.sseclient.feedbackchannel.BroadcastedEventType;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncManagerImpl implements SyncManager, BroadcastedEventListener {

    private final SplitClientConfig mSplitClientConfig;
    private final PushManagerEventBroadcaster mPushManagerEventBroadcaster;
    private final Synchronizer mSynchronizer;
    private final PushNotificationManager mPushNotificationManager;
    private SplitUpdatesWorker mSplitUpdateWorker;
    private MySegmentsUpdateWorker mMySegmentUpdateWorker;


    private AtomicBoolean mIsPushEnabled;

    public SyncManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                           @NonNull Synchronizer synchronizer,
                           @NonNull PushNotificationManager pushNotificationManager,
                           @NonNull SplitUpdatesWorker splitUpdateWorker,
                           @NonNull MySegmentsUpdateWorker mySegmentUpdateWorker,
                           @NonNull PushManagerEventBroadcaster pushManagerEventBroadcaster) {

        mSynchronizer = checkNotNull(synchronizer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mPushNotificationManager = checkNotNull(pushNotificationManager);
        mSplitUpdateWorker = checkNotNull(splitUpdateWorker);
        mMySegmentUpdateWorker = checkNotNull(mySegmentUpdateWorker);
        mPushManagerEventBroadcaster = checkNotNull(pushManagerEventBroadcaster);

        mIsPushEnabled = new AtomicBoolean(false);
    }


    @Override
    public void start() {

        mSynchronizer.loadSplitsFromCache();
        mSynchronizer.loadMySegmentsFromCache();

        mIsPushEnabled.set(mSplitClientConfig.streamingEnabled());
        if (mSplitClientConfig.streamingEnabled()) {
            mSynchronizer.synchronizeSplits();
            mSynchronizer.syncronizeMySegments();
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
    }

    @Override
    public void resume() {
        mSynchronizer.resume();
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
    public void onEvent(BroadcastedEvent message) {
        if (BroadcastedEventType.PUSH_DISABLED.equals(message.getMessage())
                && mIsPushEnabled.get()) {
            mIsPushEnabled.set(false);
            mSynchronizer.startPeriodicFetching();
        } else {
            mSynchronizer.stopPeriodicFetching();
            mIsPushEnabled.set(true);
        }
    }
}
