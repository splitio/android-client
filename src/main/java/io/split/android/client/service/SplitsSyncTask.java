package io.split.android.client.service;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.SplitTask;
import io.split.android.client.service.splits.HttpSplitFetcher;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitFetcherV2;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitsSyncTask implements SplitTask {

    SplitFetcherV2 mSplitFetcher;
    SplitsStorage mSplitsStorage;
    SplitChangeProcessor mSplitChangeProcessor;

    public SplitsSyncTask(SplitFetcherV2 splitFetcher, SplitsStorage splitsStorage) {
        checkNotNull(splitFetcher);
        checkNotNull(splitsStorage);

        mSplitFetcher = splitFetcher;
        mSplitsStorage = splitsStorage;
    }

    @Override
    public void execute() {
        try {
            // TODO: Ask tincho why mSplitsStorage.getTill()
            SplitChange splitChange = mSplitFetcher.execute(mSplitsStorage.getTill());
            mSplitsStorage.update(mSplitChangeProcessor.process(splitChange));
        } catch (IllegalStateException e) {
            Logger.e("Error while executing splits sync task: " + e.getLocalizedMessage());
        } catch (Exception e) {

        }
    }

    private void logError(String message) {
        Logger.e("Error while executing splits sync task: " + message);
    }
}
