package io.split.android.client.service.impressions;

import androidx.annotation.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.split.android.client.impressions.Impression;

public class ImpressionsObserver {

    private final Cache<Long, Long> mCache;

    public ImpressionsObserver(long size) {
        mCache = CacheBuilder.newBuilder()
                .maximumSize(size)
                .concurrencyLevel(4)  // Just setting the default value explicitly
                .build();
    }

    @Nullable
    public Long testAndSet(Impression impression) {
        if (null == impression) {
            return null;
        }

        Long hash = ImpressionHasher.process(impression);
        Long previous = mCache.getIfPresent(hash);
        mCache.put(hash, impression.time());
        return (previous == null ? null : Math.min(previous, impression.time()));
    }
}
