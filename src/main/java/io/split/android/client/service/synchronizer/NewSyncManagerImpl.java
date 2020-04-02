package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackListener;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;
import io.split.android.client.service.sseclient.reactor.MySegmentsUpdateWorker;
import io.split.android.client.service.sseclient.reactor.SplitUpdatesWorker;

import static com.google.common.base.Preconditions.checkNotNull;

public class NewSyncManagerImpl implements NewSyncManager, SyncManagerFeedbackListener {

    private final SplitClientConfig mSplitClientConfig;
    private final SyncManagerFeedbackChannel mSyncManagerFeedbackChannel;
    private final Synchronizer mSynchronizer;
    private final PushNotificationManager mPushNotificationManager;
    private SplitUpdatesWorker mSplitUpdateWorker;
    private MySegmentsUpdateWorker mMySegmentUpdateWorker;


    private AtomicBoolean mIsPushEnabled;

    public NewSyncManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                              @NonNull Synchronizer synchronizer,
                              @NonNull PushNotificationManager pushNotificationManager,
                              @NonNull SplitUpdatesWorker splitUpdateWorker,
                              @NonNull MySegmentsUpdateWorker mySegmentUpdateWorker,
                              @NonNull SyncManagerFeedbackChannel syncManagerFeedbackChannel) {

        mSynchronizer = checkNotNull(synchronizer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mPushNotificationManager = checkNotNull(pushNotificationManager);
        mSplitUpdateWorker = checkNotNull(splitUpdateWorker);
        mMySegmentUpdateWorker = checkNotNull(mySegmentUpdateWorker);
        mSyncManagerFeedbackChannel = checkNotNull(syncManagerFeedbackChannel);

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
            mSyncManagerFeedbackChannel.register(this);
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
    public void onFeedbackMessage(SyncManagerFeedbackMessage message) {
        if (SyncManagerFeedbackMessageType.PUSH_DISABLED.equals(message.getMessage())
                && mIsPushEnabled.get()) {
            mIsPushEnabled.set(false);
            mSynchronizer.startPeriodicFetching();
        } else {
            mSynchronizer.stopPeriodicFetching();
            mIsPushEnabled.set(true);
        }
    }
}
