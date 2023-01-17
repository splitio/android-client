package io.split.android.client;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.ImpressionManager;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.storage.events.EventsStorage;
import io.split.android.client.storage.impressions.ImpressionsStorage;
import io.split.android.fake.SplitTaskExecutorStub;

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

    private SplitTaskExecutor mTaskExecutor;

    private UserConsentManager mManager;

    @Before
    public void setup() {
        mTaskExecutor = new SplitTaskExecutorStub();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void initGranted() {
        createUserConsentManager(UserConsent.GRANTED);

        Assert.assertEquals(UserConsent.GRANTED, mSplitConfig.userConsent());
        verify(mEventsTracker, times(1)).enableTracking(true);
        verify(mImpressionManager, times(1)).enableTracking(true);
        verify(mImpressionsStorage, times(1)).enablePersistence(true);
        verify(mEventsStorage, times(1)).enablePersistence(true);
        verify(mSyncManager, times(1)).setupUserConsent(UserConsent.GRANTED);
    }

    @Test
    public void initDeclined() {
        createUserConsentManager(UserConsent.DECLINED);

        Assert.assertEquals(UserConsent.DECLINED, mSplitConfig.userConsent());
        verify(mEventsTracker, times(1)).enableTracking(false);
        verify(mImpressionManager, times(1)).enableTracking(false);
        verify(mImpressionsStorage, times(1)).enablePersistence(false);
        verify(mEventsStorage, times(1)).enablePersistence(false);
        verify(mSyncManager, times(1)).setupUserConsent(UserConsent.DECLINED);
    }

    @Test
    public void initUnknown() {
        createUserConsentManager(UserConsent.UNKNOWN);

        Assert.assertEquals(UserConsent.UNKNOWN, mSplitConfig.userConsent());
        verify(mEventsTracker, times(1)).enableTracking(true);
        verify(mImpressionManager, times(1)).enableTracking(true);
        verify(mEventsStorage, times(1)).enablePersistence(false);
        verify(mImpressionsStorage, times(1)).enablePersistence(false);
        verify(mEventsStorage, times(1)).enablePersistence(false);
        verify(mSyncManager, times(1)).setupUserConsent(UserConsent.UNKNOWN);
    }

    @Test
    public void setDeclined() {
        createUserConsentManager(UserConsent.GRANTED);
        Mockito.reset();

        mManager.setStatus(UserConsent.DECLINED);

        Assert.assertEquals(UserConsent.DECLINED, mSplitConfig.userConsent());
        verify(mEventsTracker, times(1)).enableTracking(false);
        verify(mImpressionManager, times(1)).enableTracking(false);
        verify(mImpressionsStorage, times(1)).enablePersistence(false);
        verify(mEventsStorage, times(1)).enablePersistence(false);
        verify(mSyncManager, times(1)).setupUserConsent(UserConsent.DECLINED);
    }

    @Test
    public void setUnknown() {
        createUserConsentManager(UserConsent.GRANTED);

        mManager.setStatus(UserConsent.UNKNOWN);

        Assert.assertEquals(UserConsent.UNKNOWN, mSplitConfig.userConsent());
        verify(mEventsTracker, times(2)).enableTracking(true);
        verify(mImpressionManager, times(2)).enableTracking(true);
        verify(mEventsStorage, times(1)).enablePersistence(false);
        verify(mImpressionsStorage, times(1)).enablePersistence(false);
        verify(mEventsStorage, times(1)).enablePersistence(false);
        verify(mSyncManager, times(1)).setupUserConsent(UserConsent.UNKNOWN);
    }

    @Test
    public void setGranted() {
        createUserConsentManager(UserConsent.UNKNOWN);

        mManager.setStatus(UserConsent.GRANTED);

        Assert.assertEquals(UserConsent.GRANTED, mSplitConfig.userConsent());
        verify(mEventsTracker, times(2)).enableTracking(true);
        verify(mImpressionManager, times(2)).enableTracking(true);
        verify(mImpressionsStorage, times(1)).enablePersistence(true);
        verify(mEventsStorage, times(1)).enablePersistence(true);
        verify(mSyncManager, times(1)).setupUserConsent(UserConsent.GRANTED);
    }

    @Test
    public void telemetryValues() {
        Assert.assertEquals(1, UserConsent.UNKNOWN.intValue());
        Assert.assertEquals(2, UserConsent.GRANTED.intValue());
        Assert.assertEquals(3, UserConsent.DECLINED.intValue());
    }

    private void createUserConsentManager(UserConsent status) {
        mSplitConfig = SplitClientConfig.builder().userConsent(status).build();

        mManager = new UserConsentManagerImpl(mSplitConfig,
                mImpressionsStorage,
                mEventsStorage, mSyncManager,
                mEventsTracker,
                mImpressionManager,
                mTaskExecutor);
    }
}
