package io.split.android.client.service.impressions;

import static io.split.android.client.utils.Utils.getAsInt;

import android.os.SystemClock;
import android.util.LruCache;

import androidx.annotation.Nullable;

import io.split.android.client.impressions.Impression;
import io.split.android.client.storage.db.ImpressionsObserverDao;

public class ImpressionsObserver implements ImpressionsObserverContract {

    private final ImpressionsObserverCache mCache;

    private LruCache<Long, Long> mBackupCache;

    public ImpressionsObserver(long size, ImpressionsObserverDao impressionsObserverDao) {
        mCache = new ImpressionsObserverCacheImpl(getAsInt(size), impressionsObserverDao);
    }

    public ImpressionsObserver(long size) {
        mBackupCache = new ListenableLruCache<>(getAsInt(size), null);
        mCache = null;
    }

    @Nullable
    public Long testAndSet(Impression impression) {
        if (null == impression) {
            return null;
        }

        Long hash = ImpressionHasher.process(impression);
        @Nullable
        Long previous = (mCache != null) ? mCache.get(hash) : mBackupCache.get(hash);
        if (mCache != null) {
            mCache.put(hash, impression.time());
        } else {
            mBackupCache.put(hash, impression.time());
        }

        return (previous == null ? null : Math.min(previous, impression.time()));
    }
}
