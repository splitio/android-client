package io.split.android.client.service.sseclient.notifications.mysegments;

interface SyncDelayCalculator {
    long calculateSyncDelay(String mUserKey, Long updateIntervalMs, Integer algorithmSeed);
}
