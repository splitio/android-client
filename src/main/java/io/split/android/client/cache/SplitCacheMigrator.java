package io.split.android.client.cache;

import java.util.List;

import io.split.android.client.dtos.Split;

public interface SplitCacheMigrator {
    List<Split> getAll();
    long getChangeNumber();
}
