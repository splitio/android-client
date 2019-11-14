package io.split.android.client.service;

import androidx.annotation.NonNull;

import io.split.android.client.service.splits.SplitFetcherV2;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitFetcherProvider {
    private final SplitFetcherV2 mSplitFetcher;

    public SplitFetcherProvider(@NonNull SplitFetcherV2 splitFetcher) {
        checkNotNull(splitFetcher);

        mSplitFetcher = splitFetcher;
    }

    public SplitFetcherV2 getSplitFetcher() {
        return mSplitFetcher;
    }
}
