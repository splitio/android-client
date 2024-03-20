package io.split.android.client.service.impressions.observer;

public interface PersistentImpressionsObserverCacheStorage extends ImpressionsObserverCache, ListenableLruCache.RemovalListener<Long> {

    void deleteOutdated(long timestamp);
}
