package io.split.android.client.storage.common;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.submitter.RecorderStorage;
import io.split.android.client.submitter.StoragePusher;

public interface PersistentStorage<T> extends StoragePusher<T>, RecorderStorage<T> {

    // Push method is defined in StoragePusher interface
    void pushMany(@NonNull List<T> elements);

    // pop, delete, and setActive are inherited from RecorderStorage

    void deleteInvalid(long maxTimestamp);
}
