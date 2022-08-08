package io.split.android.client.service.synchronizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MySegmentsChangeChecker {
    public boolean mySegmentsHaveChanged(List<String> oldSegments, List<String> newSegments) {
        List<String> oldValues = new ArrayList<>(oldSegments);
        List<String> newValues = new ArrayList<>(newSegments);
        Collections.sort(oldValues);
        Collections.sort(newValues);
        return !oldValues.equals(newValues);
    }
}
