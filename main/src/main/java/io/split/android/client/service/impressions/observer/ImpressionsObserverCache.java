package io.split.android.client.service.impressions.observer;

import androidx.annotation.Nullable;

interface ImpressionsObserverCache {

    @Nullable
    Long get(long hash);

    void put(long hash, long time);

    void persist();
}
