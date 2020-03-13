package io.split.android.client.cache;

import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.MySegment;

public interface MySegmentsCacheMigrator {
    /**
     * Returns all segments without filtering by key.
     * This methods is inteded to reuse legacy code
     * while migrating to new storage implementation
     */
    Map<String, List<MySegment>> getAllMySegments();

    /**
     * Removes all data from disk
     */
    void deleteAllFiles();
}
