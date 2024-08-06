package io.split.android.client.storage.mysegments;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmptyMySegmentsStorage implements MySegmentsStorage{
    @Override
    public void loadLocal() {
    }

    @Override
    public Set<String> getAll() {
        return new HashSet<>();
    }

    @Override
    public void set(@NonNull List<String> mySegments) {
    }

    @Override
    public long getTill() {
        return -1;
    }

    @Override
    public void setTill(long till) {
    }

    @Override
    public void clear() {
    }
}
