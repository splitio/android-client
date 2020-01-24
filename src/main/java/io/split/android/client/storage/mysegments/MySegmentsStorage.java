package io.split.android.client.storage.mysegments;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

public interface MySegmentsStorage {
    void loadFromDisk();
    Set<String> getAll();
    void set(@NonNull List<String> mySegments);
    void clear();
}
