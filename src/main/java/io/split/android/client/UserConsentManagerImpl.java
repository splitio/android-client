package io.split.android.client;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.ImpressionManager;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.storage.events.EventsStorage;
import io.split.android.client.storage.impressions.ImpressionsStorage;
import io.split.android.client.utils.logger.Logger;

public class UserConsentManagerImpl implements UserConsentManager {
    private final SplitClientConfig mSplitConfig;
    private final ImpressionsStorage mImpressionsStorage;
    private final EventsStorage mEventsStorage;
    private final SyncManager mSyncManager;
    private final SplitFactoryImpl.EventsTrackerProvider mEventsTracker;
    private final ImpressionManager mImpressionManager;
    private UserConsent mCurrentStatus;
    private final SplitTaskExecutor mTaskExecutor;
    private final Object mLock = new Object();

    public UserConsentManagerImpl(@NonNull SplitClientConfig splitConfig,
                                  @NonNull ImpressionsStorage impressionsStorage,
                                  @NonNull EventsStorage eventsStorage,
                                  @NonNull SyncManager syncManager,
                                  @NonNull SplitFactoryImpl.EventsTrackerProvider eventsTracker,
                                  @NonNull ImpressionManager impressionManager,
                                  @NonNull SplitTaskExecutor taskExecutor) {
        mSplitConfig = checkNotNull(splitConfig);
        mImpressionsStorage = checkNotNull(impressionsStorage);
        mEventsStorage = checkNotNull(eventsStorage);
        mSyncManager = checkNotNull(syncManager);
        mEventsTracker = checkNotNull(eventsTracker);
        mImpressionManager = checkNotNull(impressionManager);
        mTaskExecutor = taskExecutor;
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            setStatus(splitConfig.userConsent());
        }).start();
    }

    public void setStatus(UserConsent status) {

        synchronized (mLock) {
            if (mCurrentStatus == status) {
                return;
            }

            mSplitConfig.setUserConsent(status);
            enableTracking(status);
            enablePersistence(status);
            mSyncManager.setupUserConsent(status);
            mCurrentStatus = status;
            Logger.d("User consent set to " + status.toString());
        }
    }

    public UserConsent getStatus() {
        synchronized (mLock) {
            return mCurrentStatus;
        }
    }

    private void enableTracking(UserConsent status) {
        final boolean enable = (status != UserConsent.DECLINED);
        mEventsTracker.getEventsTracker().enableTracking(enable);
        mImpressionManager.enableTracking(enable);
        Logger.d("Tracking has been set to " + enable );
    }

    private void enablePersistence(UserConsent status) {
        final boolean enable = (status == UserConsent.GRANTED);
        mTaskExecutor.submit(new SplitTask() {
            @NonNull
            @Override
            public SplitTaskExecutionInfo execute() {
                mImpressionsStorage.enablePersistence(enable);
                mEventsStorage.enablePersistence(enable);
                return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
            }
        }, null);
        Logger.d("Persistence has been set to " + enable );
    }
}
