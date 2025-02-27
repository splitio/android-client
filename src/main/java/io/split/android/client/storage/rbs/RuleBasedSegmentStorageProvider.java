package io.split.android.client.storage.rbs;

import androidx.annotation.NonNull;

/**
 * Provider interface for RuleBasedSegmentStorage.
 * Used to break circular dependency between ParserCommons and RuleBasedSegmentStorage.
 */
public interface RuleBasedSegmentStorageProvider {
    /**
     * Get the RuleBasedSegmentStorage instance.
     * @return The RuleBasedSegmentStorage instance
     */
    @NonNull
    RuleBasedSegmentStorage get();
}
