package io.split.android.client.service.impressions.observer;

import android.util.LruCache;

import androidx.annotation.Nullable;

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

    interface RemovalListener<T> {

        void onRemoval(T key);
    }
}
