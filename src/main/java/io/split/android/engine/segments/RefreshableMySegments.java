package io.split.android.engine.segments;

import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.utils.Logger;
import io.split.android.engine.experiments.FetcherPolicy;

import static com.google.common.base.Preconditions.checkNotNull;

public class RefreshableMySegments implements Runnable, MySegments {

    private final Object _lock = new Object();
    private List<MySegment> _mySegments;
    private MySegmentsFetcher _mySegmentsFetcher;
    private String _matchingKey;
    private boolean _firstLoad = true;
    private final ISplitEventsManager _eventsManager;

    public RefreshableMySegments(String matchingKey, MySegmentsFetcher mySegmentsFetcher, ISplitEventsManager eventsManager) {
        _mySegmentsFetcher = mySegmentsFetcher;
        _matchingKey = matchingKey;
        _eventsManager = eventsManager;

        checkNotNull(_mySegmentsFetcher);
        checkNotNull(_matchingKey);
        checkNotNull(_eventsManager);

        initializaFromCache();
    }

    private void initializaFromCache(){
        _mySegments = _mySegmentsFetcher.fetch(_matchingKey, FetcherPolicy.CacheOnly);
        if (_mySegments != null && !_mySegments.isEmpty()) {
            _eventsManager.notifyInternalEvent(SplitInternalEvent.MYSEGEMENTS_ARE_READY);
        }
    }

    public static RefreshableMySegments create(String matchingKey, MySegmentsFetcher mySegmentsFetcher, ISplitEventsManager eventsManager) {
        return new RefreshableMySegments(matchingKey, mySegmentsFetcher, eventsManager);
    }

    @Override
    public boolean contains(String segmentName) {

        MySegment mySegment = new MySegment();
        mySegment.name = segmentName;

        if (_mySegments != null) {
            return _mySegments.contains(mySegment);
        }

        return false;
    }

    @Override
    public void forceRefresh() {
        run();
    }

    @Override
    public void run() {
        try {
            runWithoutExceptionHandling();

            if (_firstLoad) {
                _eventsManager.notifyInternalEvent(SplitInternalEvent.MYSEGEMENTS_ARE_READY);
                _firstLoad = false;
            } else {
                _eventsManager.notifyInternalEvent(SplitInternalEvent.MYSEGEMENTS_ARE_UPDATED);
            }
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
