package io.split.android.client.storage.impressions;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.storage.StoragePusher;

public interface PersistentImpressionsCountStorage extends StoragePusher<ImpressionsCountPerFeature> {
    // Push method is defined in StoragePusher interface
    void pushMany(@NonNull List<ImpressionsCountPerFeature> counts);
    List<ImpressionsCountPerFeature> pop(int count);
    void setActive(@NonNull List<ImpressionsCountPerFeature> counts);
    void delete(@NonNull List<ImpressionsCountPerFeature> counts);
    void deleteInvalid(long maxTimestamp);
}