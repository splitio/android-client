package io.split.android.client.service.synchronizer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.SplitClientConfig;

public class SyncGuardianImplTest {

    private SyncGuardianImpl mSyncGuardian;
    private SyncGuardianImpl.TimestampProvider mTimestampProvider;
    private SplitClientConfig mSplitConfig;

    @Before
    public void setUp() {
        mTimestampProvider = mock(SyncGuardianImpl.TimestampProvider.class);
        mSplitConfig = mock(SplitClientConfig.class);
    }

    @Test
    public void mustSyncReturnsFalseWhenSyncIsDisabled() {
        when(mSplitConfig.syncEnabled()).thenReturn(false);
        when(mSplitConfig.streamingEnabled()).thenReturn(true);
        when(mTimestampProvider.get()).thenReturn(2000L);
        mSyncGuardian = new SyncGuardianImpl(mSplitConfig, mTimestampProvider);
        mSyncGuardian.initialize();
        mSyncGuardian.updateLastSyncTimestamp();

        assertFalse(mSyncGuardian.mustSync());
    }

    @Test
    public void mustSyncReturnsFalseWhenStreamingIsDisabled() {
        when(mSplitConfig.syncEnabled()).thenReturn(true);
        when(mSplitConfig.streamingEnabled()).thenReturn(false);
        when(mTimestampProvider.get()).thenReturn(2000L);
        mSyncGuardian = new SyncGuardianImpl(mSplitConfig, mTimestampProvider);
        mSyncGuardian.initialize();
        mSyncGuardian.updateLastSyncTimestamp();

        assertFalse(mSyncGuardian.mustSync());
    }

    @Test
    public void mustSyncReturnsFalseWhenDiffBetweenLastSyncIsLessThanMaxSyncPeriod() {
        when(mSplitConfig.syncEnabled()).thenReturn(true);
        when(mSplitConfig.streamingEnabled()).thenReturn(true);
        when(mTimestampProvider.get()).thenReturn(100L, 150L);
        mSyncGuardian = new SyncGuardianImpl(mSplitConfig, mTimestampProvider);

        mSyncGuardian.updateLastSyncTimestamp();

        assertFalse(mSyncGuardian.mustSync());
    }

    @Test
    public void mustSyncReturnsTrueWhenDiffBetweenLastSyncIsGreaterThanMaxSyncPeriod() {
        when(mSplitConfig.syncEnabled()).thenReturn(true);
        when(mSplitConfig.streamingEnabled()).thenReturn(true);
        when(mTimestampProvider.get()).thenReturn(100L, 201L);
        mSyncGuardian = new SyncGuardianImpl(mSplitConfig, mTimestampProvider);

        mSyncGuardian.initialize();
        mSyncGuardian.updateLastSyncTimestamp();

        assertTrue(mSyncGuardian.mustSync());
    }

    @Test
    public void setMaxSyncPeriodDoesNotChangeMaxSyncPeriodWhenItIsLowerThanTheDefault() {
        when(mSplitConfig.syncEnabled()).thenReturn(true);
        when(mSplitConfig.streamingEnabled()).thenReturn(true);
        when(mSplitConfig.defaultSSEConnectionDelay()).thenReturn(60L);
        when(mTimestampProvider.get()).thenReturn(100L, 150L);
        mSyncGuardian = new SyncGuardianImpl(mSplitConfig, mTimestampProvider);
        mSyncGuardian.initialize();
        mSyncGuardian.setMaxSyncPeriod(50L);
        mSyncGuardian.updateLastSyncTimestamp();

        assertFalse(mSyncGuardian.mustSync());
    }

    @Test
    public void setMaxSyncPeriodChangesMaxSyncPeriodWhenItIsHigherThanTheDefault() {
        when(mSplitConfig.syncEnabled()).thenReturn(true);
        when(mSplitConfig.streamingEnabled()).thenReturn(true);
        when(mSplitConfig.defaultSSEConnectionDelay()).thenReturn(60L);
        when(mTimestampProvider.get()).thenReturn(100L, 180L);
        mSyncGuardian = new SyncGuardianImpl(mSplitConfig, mTimestampProvider);
        mSyncGuardian.initialize();
        mSyncGuardian.setMaxSyncPeriod(80L);
        mSyncGuardian.updateLastSyncTimestamp();

        assertTrue(mSyncGuardian.mustSync());
    }

    @Test
    public void mustSyncAlwaysReturnsFalseWhenSyncGuardianHasNotBeenInitialized() {
        when(mSplitConfig.syncEnabled()).thenReturn(true);
        when(mSplitConfig.streamingEnabled()).thenReturn(true);
        when(mTimestampProvider.get()).thenReturn(100L, 300L);
        mSyncGuardian = new SyncGuardianImpl(mSplitConfig, mTimestampProvider);

        boolean firstAttempt = mSyncGuardian.mustSync();
        boolean secondAttempt = mSyncGuardian.mustSync();
        mSyncGuardian.initialize();
        boolean thirdAttempt = mSyncGuardian.mustSync();
        assertFalse(firstAttempt);
        assertFalse(secondAttempt);
        assertTrue(thirdAttempt);
    }
}
