package io.split.android.client.service.synchronizer;

import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.SplitClientConfig;

public class SyncGuardianImpl implements SyncGuardian {

    private final AtomicLong mDefaultMaxSyncPeriod;
    private final AtomicLong mMaxSyncPeriod;
    private final AtomicLong mLastSyncTimestamp;
    private final TimestampProvider mNewTimestamp;
    private final boolean mSyncEnabled;
    private final boolean mStreamingEnabled;

    SyncGuardianImpl(long maxSyncPeriod, SplitClientConfig splitConfig, TimestampProvider timestampProvider) {
        mDefaultMaxSyncPeriod = new AtomicLong(maxSyncPeriod);
        mMaxSyncPeriod = new AtomicLong(maxSyncPeriod);
        mLastSyncTimestamp = new AtomicLong(0);
        mSyncEnabled = splitConfig.syncEnabled();
        mStreamingEnabled = splitConfig.streamingEnabled();
        mNewTimestamp = timestampProvider != null ? timestampProvider : System::currentTimeMillis;
    }

    @Override
    public void updateLastSyncTimestamp() {
        mLastSyncTimestamp.set(mNewTimestamp.get());
    }

    @Override
    public boolean mustSync() {
        if (!mSyncEnabled || !mStreamingEnabled) {
            return false;
        }

        return mNewTimestamp.get() - mLastSyncTimestamp.get() >= mMaxSyncPeriod.get();
    }

    @Override
    public void setMaxSyncPeriod(long newPeriod) {
        mMaxSyncPeriod.set(Math.max(newPeriod, mDefaultMaxSyncPeriod.get()));
    }

    interface TimestampProvider {

        long get();
    }
}
