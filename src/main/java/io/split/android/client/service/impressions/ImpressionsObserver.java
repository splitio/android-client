package io.split.android.client.service.impressions;

import static io.split.android.client.utils.Utils.getAsInt;

import android.util.LruCache;

import androidx.annotation.Nullable;

import io.split.android.client.impressions.Impression;

public class ImpressionsObserver {

    private final LruCache<Long, Long> mCache;

    public ImpressionsObserver(long size) {
        mCache = new LruCache<>(getAsInt(size));
    }

    @Nullable
    public Long testAndSet(Impression impression) {
        if (null == impression) {
            return null;
        }

        Long hash = ImpressionHasher.process(impression);
        @Nullable
        Long previous = mCache.get(hash);
        mCache.put(hash, impression.time());

        return (previous == null ? null : Math.min(previous, impression.time()));
    }
}
