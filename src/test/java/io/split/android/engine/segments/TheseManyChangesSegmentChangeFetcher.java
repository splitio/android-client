package io.split.android.engine.segments;

import com.google.common.collect.Lists;

import io.split.android.engine.segments.SegmentChangeFetcher;
import io.split.android.client.dtos.SegmentChange;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * First call returns a change, all subsequent calls return no change.
 *
 */
public class TheseManyChangesSegmentChangeFetcher implements SegmentChangeFetcher {

    private AtomicInteger _count = new AtomicInteger(0);

    private int _theseMany;

    public TheseManyChangesSegmentChangeFetcher(int theseMany) {
        _theseMany = theseMany;
    }

    @Override
    public SegmentChange fetch(String segmentName, long changesSinceThisChangeNumber) {
        if (_count.get() >= _theseMany) {
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

        _count.incrementAndGet();

        return segmentChange;

    }

    public int howManyChangesHappened() {
        return _count.get();
    }
}
