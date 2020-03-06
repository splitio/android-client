package io.split.android.client.storage.mysegments;

import androidx.annotation.NonNull;

import java.util.List;

public interface PersistentMySegmentsStorage {
    void set(@NonNull List<String> mySegments);
    List<String> getSnapshot();
    void close();
}