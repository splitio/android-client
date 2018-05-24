package io.split.android.engine.segments;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.MySegment;
import io.split.android.engine.experiments.FetcherPolicy;

/**
 * Created by fernandomartin on 2/17/18.
 */

public class StaticMySegmentsFetcher implements MySegmentsFetcher {
    private final Map<String, StaticMySegments> segments;

    public StaticMySegmentsFetcher(Map<String, StaticMySegments> segments) {
        this.segments = Preconditions.checkNotNull(segments);
    }

    @Override
    public List<MySegment> fetch(String matchingKey) {
        StaticMySegments mySegments = segments.get(matchingKey);
        if (mySegments != null) {
            return mySegments.mySegments();
        } else {
            return Lists.newArrayList();
        }
    }

    @Override
    public List<MySegment> fetch(String matchingKey, FetcherPolicy fetcherPolicy) {
        return fetch(matchingKey);
    }
}
