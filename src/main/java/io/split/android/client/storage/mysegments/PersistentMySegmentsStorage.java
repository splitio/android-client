package io.split.android.client.storage.mysegments;

import androidx.annotation.NonNull;

import java.util.List;

public interface PersistentMySegmentsStorage {

    void set(String userKey, @NonNull List<String> mySegments, long till);

    SegmentChangeDTO getSnapshot(String userKey);

    void close();
}
