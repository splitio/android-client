package io.split.android.client.service.synchronizer;

public interface SyncGuardian {

    void setMaxSyncPeriod(long maxSyncPeriod);

    void updateLastSyncTimestamp();

    boolean mustSync();

    void initialize();
}
