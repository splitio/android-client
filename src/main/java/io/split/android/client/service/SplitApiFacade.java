package io.split.android.client.service;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpRecorder;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitApiFacade {
    private final HttpFetcher<SplitChange> mSplitFetcher;
    private final HttpFetcher<List<MySegment>> mMySegmentsFetcher;
    private final HttpRecorder<List<Event>> mEventsRecorder;

    public SplitApiFacade(@NonNull HttpFetcher<SplitChange> splitFetcher,
                          @NonNull HttpFetcher<List<MySegment>> mySegmentsFetcher,
                          @NonNull HttpRecorder<List<Event>> eventsRecorder) {
        mSplitFetcher = checkNotNull(splitFetcher);
        mMySegmentsFetcher = checkNotNull(mySegmentsFetcher);
        mEventsRecorder = checkNotNull(eventsRecorder);
    }

    public HttpFetcher<SplitChange> getSplitFetcher() {
        return mSplitFetcher;
    }

    public HttpFetcher<List<MySegment>> getMySegmentsFetcher() {
        return mMySegmentsFetcher;
    }

    public HttpRecorder<List<Event>> getEventsRecorder() {
        return mEventsRecorder;
    }
}
