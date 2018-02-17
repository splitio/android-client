package io.split.android.engine.segments;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.MySegment;

/**
 * Created by fernandomartin on 2/17/18.
 */

public class StaticMySegmentsFetcher implements MySegmentsFetcher {
    private final Map<String, StaticSegment> segments;

    public StaticMySegmentsFetcher(Map<String, StaticSegment> segments) {
        this.segments = Preconditions.checkNotNull(segments);
    }

    @Override
    public List<MySegment> fetch(String matchingKey) {
        return null;
    }
}
