package io.split.android.client.storage.mysegments;

import androidx.annotation.VisibleForTesting;

import java.util.Set;

import io.split.android.client.dtos.SegmentsChange;

public interface MySegmentsStorage {
    void loadLocal();

    Set<String> getAll();

    void set(SegmentsChange segmentsChange);

    long getTill();

    @VisibleForTesting
    void clear();
}
