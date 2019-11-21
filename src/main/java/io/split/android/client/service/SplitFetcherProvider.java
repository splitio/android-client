package io.split.android.client.service;

import androidx.annotation.NonNull;

import io.split.android.client.service.mysegments.MySegmentsFetcherV2;
import io.split.android.client.service.splits.SplitFetcherV2;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitFetcherProvider {
    private final SplitFetcherV2 mSplitFetcher;
    private final MySegmentsFetcherV2 mMySegmentsFetcher;

    public SplitFetcherProvider(@NonNull SplitFetcherV2 splitFetcher,
                                @NonNull MySegmentsFetcherV2 mySegmentsFetcher) {
        checkNotNull(splitFetcher);
        checkNotNull(mySegmentsFetcher);

        mSplitFetcher = splitFetcher;
        mMySegmentsFetcher = mySegmentsFetcher;
    }

    public SplitFetcherV2 getSplitFetcher() {
        return mSplitFetcher;
    }
}
