package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitsUpdateTask implements SplitTask {

    static final String SINCE_PARAM = "since";
    private final HttpFetcher<SplitChange> mSplitFetcher;
    private final SplitsStorage mSplitsStorage;
    private final SplitChangeProcessor mSplitChangeProcessor;
    private Long mChangeNumber;
    private static final int RETRY_BASE = 1;

    public SplitsUpdateTask(HttpFetcher<SplitChange> splitFetcher,
                            SplitsStorage splitsStorage,
                            SplitChangeProcessor splitChangeProcessor,
                            long since) {
        mSplitFetcher = checkNotNull(splitFetcher);
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitChangeProcessor = checkNotNull(splitChangeProcessor);
        mChangeNumber = since;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {

        if (mChangeNumber == null || mChangeNumber == 0) {
            logError("Could not update split. Invalid change number " + mChangeNumber);
            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        }

        long storedChangeNumber = mSplitsStorage.getTill();
        if (mChangeNumber <= storedChangeNumber) {
            Logger.d("Received change number is previous than stored one. " +
                    "Avoiding update.");
            return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
        }

        ReconnectBackoffCounter backoffCounter = new ReconnectBackoffCounter(RETRY_BASE);
        boolean success = false;
        Map<String, Object> params = new HashMap<>();
        params.put(SINCE_PARAM, storedChangeNumber);
        while(!success) {
            try {

                SplitChange splitChange = mSplitFetcher.execute(params);
                mSplitsStorage.update(mSplitChangeProcessor.process(splitChange));
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

    private void logError(String message) {
        Logger.e("Error while executing splits sync task: " + message);
    }
}
