package io.split.android.client.service.impressions.observer;

import androidx.annotation.Nullable;

interface ImpressionsObserverCachePersistentStorage {

    void insert(long hash, long time);

    @Nullable
    Long get(long hash);

    void deleteOutdated(long timestamp);
}
