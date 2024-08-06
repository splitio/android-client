package io.split.android.client.storage.mysegments;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.Set;

public interface MySegmentsStorage {
    void loadLocal();

    Set<String> getAll();

    void set(@NonNull List<String> mySegments);

    long getTill();

    void setTill(long till);

    @VisibleForTesting
    void clear();
}
