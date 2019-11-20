package io.split.android.client.storage.mysegments;

import java.util.List;
import java.util.Set;

public interface MySegmentsStorage {
    Set<String> getAll();
    void set(List<String> mySegments);
    void clear();
}
