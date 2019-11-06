package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;

public interface SplitsStorage {
    Split get(@NonNull String name);
    Map<String, Split> getMany(@NonNull List<String> splits);
    void update(@NonNull List<Split> splits, long changeNumber);
    boolean isValidTrafficType(@NonNull String name);
    long getTill();
    void clear();
}
