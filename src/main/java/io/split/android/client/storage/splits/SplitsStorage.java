package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;

public interface SplitsStorage {
    void loadLocal();

    Split get(@NonNull String name);

    Map<String, Split> getMany(@NonNull List<String> splits);

    Map<String, Split> getAll();

    void update(ProcessedSplitChange splitChange);

    void updateWithoutChecks(Split split);

    boolean isValidTrafficType(@NonNull String name);

    long getTill();

    long getUpdateTimestamp();

    void clear();
}
