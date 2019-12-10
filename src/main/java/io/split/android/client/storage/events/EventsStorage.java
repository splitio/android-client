package io.split.android.client.storage.events;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

public interface EventsStorage {
    Set<String> getAll();
    void set(@NonNull List<String> mySegments);
    void clear();
}
