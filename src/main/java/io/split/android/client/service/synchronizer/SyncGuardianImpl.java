package io.split.android.client.service.synchronizer;

import androidx.annotation.VisibleForTesting;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.utils.logger.Logger;

public class SyncGuardianImpl implements SyncGuardian {

    private final AtomicLong mDefaultMaxSyncPeriod;
    private final AtomicLong mMaxSyncPeriod;
    private final AtomicLong mLastSyncTimestamp;
    private final TimestampProvider mNewTimestamp;
    private final boolean mSyncEnabled;
    private final boolean mStreamingEnabled;
    private boolean mIsInitialized = false;

    public SyncGuardianImpl(SplitClientConfig splitConfig) {
        this(splitConfig, null);
    }

    @VisibleForTesting
    SyncGuardianImpl(SplitClientConfig splitConfig, TimestampProvider timestampProvider) {
        long maxSyncPeriod = splitConfig.defaultSSEConnectionDelay();
        Logger.v("Instantiating sync guardian with max sync period: " + maxSyncPeriod + " seconds");

        mDefaultMaxSyncPeriod = new AtomicLong(maxSyncPeriod);
        mMaxSyncPeriod = new AtomicLong(maxSyncPeriod);
        mLastSyncTimestamp = new AtomicLong(0);
        mSyncEnabled = splitConfig.syncEnabled();
        mStreamingEnabled = splitConfig.streamingEnabled();
        mNewTimestamp = timestampProvider != null ? timestampProvider : () -> TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }

    @Override
    public void updateLastSyncTimestamp() {
        mLastSyncTimestamp.set(mNewTimestamp.get());
    }

    @Override
    public boolean mustSync() {
        return mIsInitialized && mSyncEnabled && mStreamingEnabled &&
                mNewTimestamp.get() - mLastSyncTimestamp.get() >= mMaxSyncPeriod.get();
    }

    @Override
    public void setMaxSyncPeriod(long newPeriod) {
        mMaxSyncPeriod.set(Math.max(newPeriod, mDefaultMaxSyncPeriod.get()));
        Logger.v("Setting new max sync period: " + mMaxSyncPeriod.get() + " seconds");
    }

    @Override
    public void initialize() {
        if (mIsInitialized) {
            return;
        }
        mIsInitialized = true;
    }

    interface TimestampProvider {

        long get();
    }
}
