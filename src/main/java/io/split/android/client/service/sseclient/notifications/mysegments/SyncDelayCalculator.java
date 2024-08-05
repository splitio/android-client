package io.split.android.client.service.sseclient.notifications.mysegments;

import io.split.android.client.service.sseclient.notifications.HashingAlgorithm;
import io.split.android.client.service.sseclient.notifications.MySegmentUpdateStrategy;

interface SyncDelayCalculator {
    long calculateSyncDelay(String key, Long updateIntervalMs, Integer algorithmSeed, MySegmentUpdateStrategy updateStrategy, HashingAlgorithm hashingAlgorithm);
}
