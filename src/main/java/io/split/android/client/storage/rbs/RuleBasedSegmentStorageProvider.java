package io.split.android.client.storage.rbs;

import androidx.annotation.NonNull;

public interface RuleBasedSegmentStorageProvider {

    @NonNull
    RuleBasedSegmentStorage get();
}
