package io.split.android.client.service.impressions.observer;

import androidx.annotation.Nullable;

import io.split.android.client.impressions.Impression;

public interface ImpressionsObserver {
    @Nullable
    Long testAndSet(Impression impression);
}
