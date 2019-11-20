package io.split.android.client.storage.mysegments;

import java.util.List;

public interface PersistentMySegmentsStorage {
    void set(List<String> mySegments);
    List<String> getSnapshot();
    void close();
}