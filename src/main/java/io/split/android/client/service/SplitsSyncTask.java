package io.split.android.client.backend;

import io.split.android.client.backend.SplitTask;
import io.split.android.client.backend.splits.HttpSplitFetcher;
import io.split.android.client.backend.splits.SplitFetcherV2;
import io.split.android.client.storage.splits.SplitsStorage;

public class SplitsSyncTask implements SplitTask {

    SplitFetcherV2 mSplitFetcher;
    SplitsStorage mSplitsStorage;

    public SplitsSyncTask(SplitFetcherV2 splitFetcher, SplitsStorage splitsStorage) {
        mSplitFetcher = splitFetcher;
        mSplitsStorage = splitsStorage;
    }

    @Override
    public void execute() {

    }
}
