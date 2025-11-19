package io.split.android.client.service.impressions.observer;

import android.util.LruCache;

import androidx.annotation.Nullable;

/**
 * Extension of {@link LruCache} that allows to listen to cache evictions.
 * <p>
 * The listener is notified when an entry is evicted from the cache by informing the key of the
 * evicted entry.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
class ListenableLruCache<K, V> extends LruCache<K, V> {

    private final RemovalListener<K> mRemovalListener;

    public ListenableLruCache(int maxSize, @Nullable RemovalListener<K> removalListener) {
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

    /**
     * Listener to be notified when an entry is evicted from the cache.
     *
     * @param <T> Key type
     */
    interface RemovalListener<T> {

        /**
         * Called when an entry is evicted from the cache.
         *
         * @param key The key of the evicted entry
         */
        void onRemoval(T key);
    }
}
