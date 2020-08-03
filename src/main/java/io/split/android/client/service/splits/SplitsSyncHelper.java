package io.split.android.client.service.splits;

import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitsSyncHelper {

    private final HttpFetcher<SplitChange> mSplitFetcher;
    private final SplitsStorage mSplitsStorage;
    private final SplitChangeProcessor mSplitChangeProcessor;
    private static final int RETRY_BASE = 1;

    public SplitsSyncHelper(HttpFetcher<SplitChange> splitFetcher,
                            SplitsStorage splitsStorage,
                            SplitChangeProcessor splitChangeProcessor) {
        mSplitFetcher = checkNotNull(splitFetcher);
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitChangeProcessor = checkNotNull(splitChangeProcessor);
    }

    public SplitTaskExecutionInfo syncUntilSuccess(Map<String, Object> params) {
        return syncUntilSuccess(params, false);
    }

    public SplitTaskExecutionInfo syncUntilSuccess(Map<String, Object> params, boolean clearBeforeUpdate) {

        ReconnectBackoffCounter backoffCounter = new ReconnectBackoffCounter(RETRY_BASE);
        boolean success = false;

        while (!success) {
            try {
                SplitChange splitChange = mSplitFetcher.execute(params);
                ProcessedSplitChange processedSplitChange = mSplitChangeProcessor.process(splitChange);
                if (clearBeforeUpdate) {
                    mSplitsStorage.clear();
                }
                mSplitsStorage.update(processedSplitChange);
                success = true;
            } catch (HttpFetcherException e) {
                logError("Network error while updating splits" + e.getLocalizedMessage());
                try {
                    Thread.sleep(backoffCounter.getNextRetryTime());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
                }
            } catch (Exception e) {
                logError("Unexpected error while updating splits" + e.getLocalizedMessage());
                return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
            }
        }
        Logger.d("Features have been updated");
        return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
    }

    public SplitTaskExecutionInfo sync(Map<String, Object> params) {
        try {
            SplitChange splitChange = mSplitFetcher.execute(params);
            mSplitsStorage.update(mSplitChangeProcessor.process(splitChange));
        } catch (HttpFetcherException e) {
            logError("Newtwork error while fetching splits" + e.getLocalizedMessage());

            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        } catch (Exception e) {
            logError("Unexpected while fetching splits" + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        }
        Logger.d("Features have been updated");
        return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
    }

    private long now() {
        return System.currentTimeMillis() / 1000;
    }

    public boolean cacheHasExpired(long storedChangeNumber, long updateTimestamp, long cacheExpirationInSeconds) {
        long elepased = now() - updateTimestamp;
        return storedChangeNumber > -1
                && updateTimestamp > 0
                && (elepased > cacheExpirationInSeconds);
    }

    private void logError(String message) {
        Logger.e("Error while executing splits syncUntilSuccess/update task: " + message);
    }
}
