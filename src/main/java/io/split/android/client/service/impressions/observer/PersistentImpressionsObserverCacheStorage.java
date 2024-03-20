package io.split.android.client.service.impressions.observer;

public interface PersistentImpressionsObserverCacheStorage extends ImpressionsObserverCache, ListenableLruCache.RemovalListener<Long> {

    /**
     * Deletes all entries older than the given timestamp
     *
     * @param timestamp in ms
     */
    void deleteOutdated(long timestamp);
}
