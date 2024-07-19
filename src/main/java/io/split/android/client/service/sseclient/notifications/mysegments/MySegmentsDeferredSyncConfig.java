package io.split.android.client.service.sseclient.notifications.mysegments;

import java.util.concurrent.TimeUnit;

import io.split.android.client.service.sseclient.notifications.HashingAlgorithm;

public class MySegmentsDeferredSyncConfig {

    private final int mAlgorithmSeed;
    private final HashingAlgorithm mHashingAlgorithm;
    private final long mUpdateIntervalMs;
    private final boolean mIgnoreConfig;

    private MySegmentsDeferredSyncConfig(int algorithmSeed, HashingAlgorithm hashingAlgorithm, long updateIntervalMs, boolean ignoreConfig) {
        mAlgorithmSeed = algorithmSeed;
        mHashingAlgorithm = hashingAlgorithm;
        mUpdateIntervalMs = updateIntervalMs;
        mIgnoreConfig = ignoreConfig;
    }

    public static MySegmentsDeferredSyncConfig createDefault(boolean ignoreConfig) {
        return new MySegmentsDeferredSyncConfig(0, HashingAlgorithm.MURMUR3_32, TimeUnit.SECONDS.toMillis(60), ignoreConfig);
    }

    public static MySegmentsDeferredSyncConfig create(Integer algorithmSeed, HashingAlgorithm hashingAlgorithm, Long updateIntervalMs, boolean ignoreConfig) {
        if (algorithmSeed == null || hashingAlgorithm == null || updateIntervalMs == null) {
            return createDefault(ignoreConfig);
        }

        return new MySegmentsDeferredSyncConfig(algorithmSeed, hashingAlgorithm, updateIntervalMs, ignoreConfig);
    }

    public long getSyncDelay() {
        return 0; // TODO
    }
}
