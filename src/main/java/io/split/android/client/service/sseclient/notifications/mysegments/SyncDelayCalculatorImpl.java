package io.split.android.client.service.sseclient.notifications.mysegments;

import java.util.concurrent.TimeUnit;

import io.split.android.client.utils.MurmurHash3;

class SyncDelayCalculatorImpl implements SyncDelayCalculator {

    public static final long DEFAULT_SYNC_INTERVAL_MS = TimeUnit.SECONDS.toMillis(60);

    @Override
    public long calculateSyncDelay(String key, Long updateIntervalMs, Integer algorithmSeed) {
        if (updateIntervalMs == null || updateIntervalMs <= 0) {
            updateIntervalMs = DEFAULT_SYNC_INTERVAL_MS;
        }

        if (algorithmSeed == null) {
            algorithmSeed = 0;
        }

        return MurmurHash3.murmurhash3_x86_32(key, 0, key.length(), algorithmSeed) % updateIntervalMs;
    }
}
