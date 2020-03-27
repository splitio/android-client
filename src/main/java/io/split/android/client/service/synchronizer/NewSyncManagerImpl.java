package io.split.android.client.service.synchronizer;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.atomic.AtomicBoolean;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.sseclient.PushNotificationManager;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackChannel;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackListener;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessage;
import io.split.android.client.service.sseclient.feedbackchannel.SyncManagerFeedbackMessageType;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO: Will be renamed to SyncManagerImpl on final integration
@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
public class NewSyncManagerImpl implements NewSyncManager, SyncManagerFeedbackListener {

    private final SplitClientConfig mSplitClientConfig;
    private final SyncManagerFeedbackChannel mSyncManagerFeedbackChannel;
    private final Synchronizer mSynchronizer;
    private final PushNotificationManager mPushNotificationManager;

    private AtomicBoolean mIsPushEnabled;

    public NewSyncManagerImpl(@NonNull SplitClientConfig splitClientConfig,
                              @NonNull Synchronizer synchronizer,
                              @NonNull PushNotificationManager pushNotificationManager,
                              @NonNull SyncManagerFeedbackChannel syncManagerFeedbackChannel) {

        mSyncManagerFeedbackChannel = checkNotNull(syncManagerFeedbackChannel);
        mSynchronizer = checkNotNull(synchronizer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mPushNotificationManager = checkNotNull(pushNotificationManager);
    }


    @Override
    public void start() {

        mSynchronizer.doInitialLoadFromCache();

        //TODO: Replace following variable with push config when initial setup implemented
        mIsPushEnabled.set(true);
        //

        if (mIsPushEnabled.get()) {
            mSyncManagerFeedbackChannel.register(this);
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
    public void stop() {
        mSynchronizer.stop();
        mPushNotificationManager.stop();
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
