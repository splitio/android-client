package io.split.android.client.service.splits;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitFetcherV2;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitsSyncTask implements SplitTask {

    private final SplitFetcherV2 mSplitFetcher;
    private final SplitsStorage mSplitsStorage;
    private final SplitChangeProcessor mSplitChangeProcessor;

    public SplitsSyncTask(SplitFetcherV2 splitFetcher, SplitsStorage splitsStorage) {
        checkNotNull(splitFetcher);
        checkNotNull(splitsStorage);

        mSplitFetcher = splitFetcher;
        mSplitsStorage = splitsStorage;
        mSplitChangeProcessor = new SplitChangeProcessor();
    }

    @Override
    public void execute() {
        try {
            // TODO: Ask tincho why mSplitsStorage.getTill()
            SplitChange splitChange = mSplitFetcher.execute(mSplitsStorage.getTill());
            mSplitsStorage.update(mSplitChangeProcessor.process(splitChange));
        } catch (IllegalStateException e) {
            logError(e.getLocalizedMessage());
        } catch (Exception e) {
            logError("unexpected " + e.getLocalizedMessage());
        }
    }

    private void logError(String message) {
        Logger.e("Error while executing splits sync task: " + message);
    }
}
