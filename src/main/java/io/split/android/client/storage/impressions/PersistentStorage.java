package io.split.android.client.storage.impressions;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.storage.StoragePusher;

public interface PersistentStorage<T> extends StoragePusher<T> {

    // Push method is defined in StoragePusher interface
    void pushMany(@NonNull List<T> elements);

    List<T> pop(int count);

    void setActive(@NonNull List<T> elements);

    void delete(@NonNull List<T> elements);

    void deleteInvalid(long maxTimestamp);
}
