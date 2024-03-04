package io.split.android.client.service.impressions;

interface ImpressionsObserverCache {

    void put(Long hash, long time);

    Long get(Long hash);
}
