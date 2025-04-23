package io.split.android.client.service.impressions.observer;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import io.split.android.client.impressions.Impression;
import io.split.android.client.service.impressions.ImpressionHasher;

public class ImpressionsObserverImpl implements ImpressionsObserver {

    private final ImpressionsObserverCache mCache;

    public ImpressionsObserverImpl(PersistentImpressionsObserverCacheStorage persistentStorage, int size) {
        this(new ImpressionsObserverCacheImpl(persistentStorage, size));
    }

    @VisibleForTesting
    ImpressionsObserverImpl(ImpressionsObserverCache cache) {
        mCache = checkNotNull(cache);
    }

    @Override
    @Nullable
    public Long testAndSet(Impression impression) {
        if (null == impression) {
            return null;
        }
        final String properties = impression.properties();
        if (properties != null && !properties.isEmpty()) {
            return null;
        }

        Long hash = ImpressionHasher.process(impression);
        @Nullable
        Long previous = mCache.get(hash);
        mCache.put(hash, impression.time());

        return (previous == null ? null : Math.min(previous, impression.time()));
    }

    @Override
    public void persist() {
        mCache.persist();
    }
}
