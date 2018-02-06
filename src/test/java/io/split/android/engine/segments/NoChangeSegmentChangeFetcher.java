package io.split.android.engine.segments;

import io.split.android.engine.segments.SegmentChangeFetcher;
import io.split.android.client.dtos.SegmentChange;

import java.util.Collections;

/**
 * First call returns a change, all subsequent calls return no change.
 *
 */
public class NoChangeSegmentChangeFetcher implements SegmentChangeFetcher {

    @Override
    public SegmentChange fetch(String segmentName, long changesSinceThisChangeNumber) {
        SegmentChange segmentChange = new SegmentChange();
        segmentChange.name = segmentName;
        segmentChange.since = changesSinceThisChangeNumber;
        segmentChange.till = changesSinceThisChangeNumber;
        segmentChange.added = Collections.<String>emptyList();
        segmentChange.removed = Collections.<String>emptyList();

        return segmentChange;

    }

}
