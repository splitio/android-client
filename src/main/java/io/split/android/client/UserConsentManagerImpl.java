package io.split.android.client;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.impressions.ImpressionManager;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.service.synchronizer.Synchronizer;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.storage.events.EventsStorage;
import io.split.android.client.storage.impressions.ImpressionsStorage;
import io.split.android.client.utils.logger.Logger;

public class UserConsentManagerImpl implements UserConsentManager {
    private final SplitClientConfig mSplitConfig;
    private final ImpressionsStorage mImpressionsStorage;
    private final EventsStorage mEventsStorage;
    private final SyncManager mSyncManager;
    private final EventsTracker mEventsTracker;
    private final ImpressionManager mImpressionManager;
    private UserConsent mCurrentStatus;

    public UserConsentManagerImpl(@NonNull SplitClientConfig splitConfig,
                                  ImpressionsStorage impressionsStorage,
                                  EventsStorage eventsStorage,
                                  SyncManager syncManager,
                                  EventsTracker eventsTracker,
                                  ImpressionManager impressionManager) {
        mSplitConfig = checkNotNull(splitConfig);
        mImpressionsStorage = checkNotNull(impressionsStorage);
        mEventsStorage = checkNotNull(eventsStorage);
        mSyncManager = checkNotNull(syncManager);
        mEventsTracker = checkNotNull(eventsTracker);
        mImpressionManager = checkNotNull(impressionManager);

    }

    public synchronized void set(UserConsent status) {
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

    private void enableTracking(UserConsent status) {
        final boolean enable = (status != UserConsent.DECLINED);
        mEventsTracker.enableTracking(enable);
        mImpressionManager.enableTracking(enable);
    }

    private void enablePersistence(UserConsent status) {
        final boolean enable = (status == UserConsent.GRANTED);
        mImpressionsStorage.enablePersistence(enable);
        mEventsStorage.enablePersistence(enable);
    }
}
