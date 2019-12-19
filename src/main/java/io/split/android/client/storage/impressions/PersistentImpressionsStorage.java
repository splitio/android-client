package io.split.android.client.storage.impressions;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;

public interface PersistentImpressionsStorage {
    void push(@NonNull KeyImpression impression);
    void pushMany(@NonNull List<KeyImpression> impression);
    List<KeyImpression> pop(int count);
    List<KeyImpression> getCritical();
}