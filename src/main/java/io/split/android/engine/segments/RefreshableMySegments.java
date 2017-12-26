package io.split.android.engine.segments;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.split.android.client.dtos.MySegment;
import timber.log.Timber;

import static com.google.common.base.Preconditions.checkNotNull;

public class RefreshableMySegments implements Runnable, MySegments {

    private List<MySegment> _mySegments;
    private MySegmentsFetcher _mySegmentsFetcher;
    private String _matchingKey;

    private final Object _lock = new Object();

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

    public static RefreshableMySegments create(String matchingKey, MySegmentsFetcher mySegmentsFetcher) {
        return new RefreshableMySegments(matchingKey, mySegmentsFetcher);
    }


    public RefreshableMySegments(String matchingKey, MySegmentsFetcher mySegmentsFetcher) {
        _mySegmentsFetcher = mySegmentsFetcher;
        _matchingKey = matchingKey;

        checkNotNull(_mySegmentsFetcher);
        checkNotNull(_matchingKey);
    }

    @Override
    public void run() {
        try {
                runWithoutExceptionHandling();
        } catch (Throwable t) {
            Timber.e("RefreshableMySegments failed: %s", t.getMessage());
            Timber.d(t);
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
