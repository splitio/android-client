package io.split.android.client.service.impressions.strategy;

import androidx.annotation.NonNull;

import io.split.android.client.impressions.Impression;

public interface ProcessStrategy extends PeriodicTracker {

    void apply(@NonNull Impression impression);
}
