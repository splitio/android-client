package io.split.android.engine.segments;

import com.google.common.collect.Maps;

import java.util.Map;

import io.split.android.engine.SDKReadinessGates;

public class StaticMySegmentsFectherProvider {

    public static RefreshableMySegmentsFetcherProvider get(String key) {
        return get(key, new SDKReadinessGates(), Maps.newHashMap());
    }

    public static RefreshableMySegmentsFetcherProvider get(String key, SDKReadinessGates gates) {
        return get(key, gates, Maps.newHashMap());
    }

    public static RefreshableMySegmentsFetcherProvider get(String key, Map<String, StaticMySegments> fetcherMap) {
        return get(key, new SDKReadinessGates(), fetcherMap);
    }

    public static RefreshableMySegmentsFetcherProvider get(String key, SDKReadinessGates gates, Map<String, StaticMySegments> fetcherMap) {
        MySegmentsFetcher segmentFetcher = new StaticMySegmentsFetcher(fetcherMap);
        return new RefreshableMySegmentsFetcherProvider(
                segmentFetcher,
                10L,
                key,
                gates);
    }
}
