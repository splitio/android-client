package io.split.android.client.storage.mysegments;

import io.split.android.client.dtos.SegmentsChange;

public interface PersistentMySegmentsStorage {

    void set(String userKey, SegmentsChange segmentsChange);

    SegmentsChange getSnapshot(String userKey);

    void close();
}
