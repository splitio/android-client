package io.split.android.client.storage.rbs;

import androidx.annotation.Nullable;

public interface RuleBasedSegmentStorageProvider {

    @Nullable
    RuleBasedSegmentStorage get();
}
