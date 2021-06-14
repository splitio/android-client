package io.split.android.client.storage.impressions;

import androidx.annotation.NonNull;

import java.util.List;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.storage.StoragePusher;

public interface PersistentImpressionsStorage extends StoragePusher<KeyImpression> {
    // Push method is defined in StoragePusher interface
    void pushMany(@NonNull List<KeyImpression> impressions);
    List<KeyImpression> pop(int count);
    void setActive(@NonNull List<KeyImpression> impressions);
    void delete(@NonNull List<KeyImpression> impressions);
    void deleteInvalid(long maxTimestamp);
}