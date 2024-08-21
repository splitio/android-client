package io.split.android.client.service.synchronizer;

import java.util.Collections;
import java.util.List;

public class MySegmentsChangeChecker {
    public boolean mySegmentsHaveChanged(final List<String> oldSegments, final List<String> newSegments) {
        Collections.sort(oldSegments);
        Collections.sort(newSegments);
        return !oldSegments.equals(newSegments);
    }
}
