package io.split.android.engine.segments;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

import io.split.android.engine.segments.Segment;
import io.split.android.engine.segments.SegmentFetcher;

/**
 * Provides fetchers of type StaticSegmentFetcher.
 *
 */
public class StaticSegmentFetcher implements SegmentFetcher {

    private final ImmutableMap<String, StaticSegment> _staticSegmentFetchers;

    public StaticSegmentFetcher(Map<String, StaticSegment> staticSegmentFetchers) {
        _staticSegmentFetchers = ImmutableMap.copyOf(staticSegmentFetchers);
    }


    @Override
    public Segment segment(String segmentName) {
        StaticSegment segmentFetcher = _staticSegmentFetchers.get(segmentName);
        if (segmentFetcher == null) {
            segmentFetcher = new StaticSegment(segmentName, Collections.<String>emptySet());
        }
        return segmentFetcher;
    }
}
