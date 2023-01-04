package io.split.android.client;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.impressions.ImpressionManager;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.storage.events.EventsStorage;
import io.split.android.client.storage.impressions.ImpressionsStorage;

public class UserConsentManagerTest {

    private SplitClientConfig mSplitConfig;

    @Mock
    private ImpressionsStorage mImpressionsStorage;
    @Mock
    private EventsStorage mEventsStorage;
    @Mock
    private SyncManager mSyncManager;
    @Mock
    private EventsTracker mEventsTracker;
    @Mock
    private ImpressionManager mImpressionManager;

    private UserConsentManager mManager;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void setDeclined() {
        createUserConsentManager(UserConsent.GRANTED);

        mManager.set(UserConsent.DECLINED);

        Assert.assertEquals(UserConsent.DECLINED, mSplitConfig.userConsent());
        verify(mEventsTracker, times(1)).enableTracking(false);
        verify(mImpressionManager, times(1)).enableTracking(false);
        verify(mEventsStorage, times(1)).enablePersistence(false);
        verify(mImpressionsStorage, times(1)).enablePersistence(false);
        verify(mSyncManager, times(1)).setupUserConsent(UserConsent.DECLINED);
    }

    @Test
    public void setUnknown() {
        createUserConsentManager(UserConsent.GRANTED);

        mManager.set(UserConsent.UNKNOWN);

        Assert.assertEquals(UserConsent.UNKNOWN, mSplitConfig.userConsent());
        verify(mEventsTracker, times(1)).enableTracking(true);
        verify(mImpressionManager, times(1)).enableTracking(true);
        verify(mEventsStorage, times(1)).enablePersistence(false);
        verify(mImpressionsStorage, times(1)).enablePersistence(false);
        verify(mSyncManager, times(1)).setupUserConsent(UserConsent.UNKNOWN);
    }

    @Test
    public void setGranted() {
        createUserConsentManager(UserConsent.UNKNOWN);

        mManager.set(UserConsent.GRANTED);

        Assert.assertEquals(UserConsent.GRANTED, mSplitConfig.userConsent());
        verify(mEventsTracker, times(1)).enableTracking(true);
        verify(mImpressionManager, times(1)).enableTracking(true);
        verify(mEventsStorage, times(1)).enablePersistence(true);
        verify(mImpressionsStorage, times(1)).enablePersistence(true);
        verify(mSyncManager, times(1)).setupUserConsent(UserConsent.GRANTED);
    }

    private void createUserConsentManager(UserConsent status) {
        mSplitConfig = SplitClientConfig.builder().userConsent(status).build();

        mManager = new UserConsentManagerImpl(mSplitConfig,
                mImpressionsStorage,
                mEventsStorage, mSyncManager,
                mEventsTracker,
                mImpressionManager);
    }
}
