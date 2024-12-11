package io.split.android.client.storage.mysegments;

import java.util.Set;

import io.split.android.client.dtos.SegmentsChange;
import io.split.android.client.storage.RolloutDefinitionsCache;

public interface MySegmentsStorage extends RolloutDefinitionsCache {
    void loadLocal();

    Set<String> getAll();

    void set(SegmentsChange segmentsChange);

    long getChangeNumber();
}
