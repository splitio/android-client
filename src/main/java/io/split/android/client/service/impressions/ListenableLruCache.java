package io.split.android.client.service.impressions;

import android.util.LruCache;

class ListenableLruCache<K, V> extends LruCache<K, V> {

    private final RemovalListener<K> mRemovalListener;

    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *                the maximum number of entries in the cache. For all other caches,
     *                this is the maximum sum of the sizes of the entries in this cache.
     */
    public ListenableLruCache(int maxSize, RemovalListener removalListener) {
        super(maxSize);
        mRemovalListener = removalListener;
    }

    @Override
    protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue) {
        super.entryRemoved(evicted, key, oldValue, newValue);
        if (mRemovalListener != null && evicted) {
            mRemovalListener.onRemoval(key);
        }
    }
}

interface RemovalListener<T> {

    void onRemoval(T key);
}
