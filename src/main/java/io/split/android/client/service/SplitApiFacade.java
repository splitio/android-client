package io.split.android.client.service;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitApiFacade {
    private final HttpFetcher<SplitChange> mSplitFetcher;
    private final HttpFetcher<List<MySegment>> mMySegmentsFetcher;


    public SplitApiFacade(@NonNull HttpFetcher<SplitChange> splitFetcher,
                          @NonNull HttpFetcher<List<MySegment>> mySegmentsFetcher) {
        mSplitFetcher = checkNotNull(splitFetcher);
        mMySegmentsFetcher = checkNotNull(mySegmentsFetcher);
    }

    public HttpFetcher<SplitChange> getSplitFetcher() {
        return mSplitFetcher;
    }

    public HttpFetcher<List<MySegment>> getmMySegmentsFetcher() {
        return mMySegmentsFetcher;
    }
}
