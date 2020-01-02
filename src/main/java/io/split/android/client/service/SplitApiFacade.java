package io.split.android.client.service;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.http.HttpFetcher;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitApiFacade {
    private final HttpFetcher<SplitChange> mSplitFetcher;


    public SplitApiFacade(@NonNull HttpFetcher<SplitChange> splitFetcher) {
        checkNotNull(splitFetcher);

        mSplitFetcher = splitFetcher;
    }

    public HttpFetcher<SplitChange> getSplitFetcher() {
        return mSplitFetcher;
    }
}
