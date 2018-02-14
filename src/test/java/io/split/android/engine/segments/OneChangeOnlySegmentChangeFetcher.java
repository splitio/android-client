package io.split.android.engine.segments;

import com.google.common.collect.Lists;

import io.split.android.engine.segments.SegmentChangeFetcher;
import io.split.android.client.dtos.SegmentChange;

import java.util.Collections;

/**
 * First call returns a change, all subsequent calls return no change.
 *
 */
public class OneChangeOnlySegmentChangeFetcher implements SegmentChangeFetcher {

    private volatile boolean _changeHappenedAlready = false;

    @Override
    public SegmentChange fetch(String segmentName, long changesSinceThisChangeNumber) {
        if (_changeHappenedAlready) {
            SegmentChange segmentChange = new SegmentChange();
            segmentChange.name = segmentName;
            segmentChange.since = changesSinceThisChangeNumber;
            segmentChange.till = changesSinceThisChangeNumber;
            segmentChange.added = Collections.<String>emptyList();
            segmentChange.removed = Collections.<String>emptyList();
            return segmentChange;
        }

        long latestChangeNumber = changesSinceThisChangeNumber + 1;

        SegmentChange segmentChange = new SegmentChange();
        segmentChange.name = segmentName;
        segmentChange.since = changesSinceThisChangeNumber;
        segmentChange.till = latestChangeNumber;
        segmentChange.added = Lists.newArrayList("" + latestChangeNumber);
        segmentChange.removed = Lists.newArrayList("" + changesSinceThisChangeNumber);

        _changeHappenedAlready = true;

        return segmentChange;

    }

    public boolean changeHappenedAlready() {
        return _changeHappenedAlready;
    }
}
