package io.split.android.client.service.impressions;

import androidx.annotation.Nullable;

import io.split.android.client.impressions.Impression;

public interface ImpressionsObserverContract {

    @Nullable
    Long testAndSet(Impression impression);
}
