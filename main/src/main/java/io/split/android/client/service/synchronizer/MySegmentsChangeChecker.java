package io.split.android.client.service.synchronizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MySegmentsChangeChecker {

    /**
     * Computes and returns the list of changed segment names (added + removed) between old and new segments.
     * An empty list means no changes occurred.
     *
     * @param oldSegments the previous list of segment names
     * @param newSegments the new list of segment names
     * @return list of segment names that were either added or removed (empty if no changes)
     */
    public List<String> getChangedSegments(final List<String> oldSegments, final List<String> newSegments) {
        Set<String> oldSet = new HashSet<>(oldSegments);
        Set<String> newSet = new HashSet<>(newSegments);

        // Added segments: in new but not in old
        Set<String> added = new HashSet<>(newSet);
        added.removeAll(oldSet);

        // Removed segments: in old but not in new
        Set<String> removed = new HashSet<>(oldSet);
        removed.removeAll(newSet);

        // Combined changed segments
        Set<String> changed = new HashSet<>(added);
        changed.addAll(removed);

        return new ArrayList<>(changed);
    }
}
