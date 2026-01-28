package io.split.android.client.storage.common;

import androidx.annotation.NonNull;

import java.util.List;

public interface PersistentStorage<T> extends StoragePusher<T> {

    // Push method is defined in StoragePusher interface
    void pushMany(@NonNull List<T> elements);

    List<T> pop(int count);

    void setActive(@NonNull List<T> elements);

    void delete(@NonNull List<T> elements);

    void deleteInvalid(long maxTimestamp);
}
