package io.split.android.client.storage.impressions;

import androidx.annotation.NonNull;

import java.util.List;
import io.split.android.client.service.impressions.ImpressionsCountPerFeature;

public interface PersistentImpressionsCountStorage {
    // Push method is defined in StoragePusher interface
    void save(@NonNull List<ImpressionsCountPerFeature> count);
    List<ImpressionsCountPerFeature> pop(int count);
    List<ImpressionsCountPerFeature> getCritical();
    void setActive(@NonNull List<ImpressionsCountPerFeature> impressions);
    void delete(@NonNull List<ImpressionsCountPerFeature> impressions);
    void deleteInvalid(long maxTimestamp);
}