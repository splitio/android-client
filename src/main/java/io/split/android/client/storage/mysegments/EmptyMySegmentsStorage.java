package io.split.android.client.storage.mysegments;

import java.util.HashSet;
import java.util.Set;

import io.split.android.client.dtos.SegmentsChange;

public class EmptyMySegmentsStorage implements MySegmentsStorage{
    @Override
    public void loadLocal() {
    }

    @Override
    public Set<String> getAll() {
        return new HashSet<>();
    }

    @Override
    public void set(SegmentsChange segmentsChange) {
    }

    @Override
    public long getTill() {
        return -1;
    }

    @Override
    public void clear() {
    }
}
