package io.split.android.client.service.impressions.observer;

public interface ImpressionsObserverCachePersistentStorage extends ImpressionsObserverCache, ListenableLruCache.RemovalListener<Long> {

    void deleteOutdated(long timestamp);
}
