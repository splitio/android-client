package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Logger;
import io.split.android.client.service.executor.SplitTask;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitsSyncTask implements SplitTask {

    static final String SINCE_PARAM = "since";
    private final HttpFetcher<SplitChange> mSplitFetcher;
    private final SplitsStorage mSplitsStorage;
    private final SplitChangeProcessor mSplitChangeProcessor;


    public SplitsSyncTask(HttpFetcher<SplitChange> splitFetcher,
                          SplitsStorage splitsStorage,
                          SplitChangeProcessor splitChangeProcessor) {
        mSplitFetcher = checkNotNull(splitFetcher);
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitChangeProcessor = checkNotNull(splitChangeProcessor);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(SINCE_PARAM, mSplitsStorage.getTill());
            SplitChange splitChange = mSplitFetcher.execute(params);
            mSplitsStorage.update(mSplitChangeProcessor.process(splitChange));
        } catch (Exception e) {
            logError("unexpected " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        }
        return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
    }

    private void logError(String message) {
        Logger.e("Error while executing splits sync task: " + message);
    }
}
