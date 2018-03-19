package io.split.android.engine.segments;

import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.utils.Logger;
import io.split.android.engine.SDKReadinessGates;
import io.split.android.engine.experiments.FetcherPolicy;

import static com.google.common.base.Preconditions.checkNotNull;

public class RefreshableMySegments implements Runnable, MySegments {

    private final SDKReadinessGates _gates;
    private final Object _lock = new Object();
    private List<MySegment> _mySegments;
    private MySegmentsFetcher _mySegmentsFetcher;
    private String _matchingKey;

    public RefreshableMySegments(String matchingKey, MySegmentsFetcher mySegmentsFetcher, SDKReadinessGates gates) {
        _mySegmentsFetcher = mySegmentsFetcher;
        _matchingKey = matchingKey;
        _gates = gates;

        checkNotNull(_mySegmentsFetcher);
        checkNotNull(_matchingKey);
        checkNotNull(_gates);

        initializaFromCache();
    }

    private void initializaFromCache(){
        _mySegments = _mySegmentsFetcher.fetch(_matchingKey, FetcherPolicy.CacheOnly);
    }

    public static RefreshableMySegments create(String matchingKey, MySegmentsFetcher mySegmentsFetcher, SDKReadinessGates gates) {
        return new RefreshableMySegments(matchingKey, mySegmentsFetcher, gates);
    }

    @Override
    public boolean contains(String segmentName) {

        MySegment mySegment = new MySegment();
        mySegment.name = segmentName;

        return _mySegments.contains(mySegment);
    }

    @Override
    public void forceRefresh() {
        run();
    }

    @Override
    public void run() {
        try {
            runWithoutExceptionHandling();

            _gates.mySegmentsAreReady();
        } catch (Throwable t) {
            Logger.e(t,"RefreshableMySegments failed: %s", t.getMessage());
        }

    }

    private void runWithoutExceptionHandling() {
        List<MySegment> mySegments = _mySegmentsFetcher.fetch(_matchingKey);

        synchronized (_lock) {
            if (mySegments != null) {
                _mySegments = mySegments;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("RefreshableMySegments[%s]", _mySegments);
    }

}
