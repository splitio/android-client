package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.splits.SplitsStorage;

import static io.split.android.client.utils.Utils.checkNotNull;

public class LoadSplitsTask implements SplitTask {

    private final SplitsStorage mSplitsStorage;
    private final String mSplitsFilterQueryStringFromConfig;

    public LoadSplitsTask(SplitsStorage splitsStorage, String splitsFilterQueryStringFromConfig) {
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitsFilterQueryStringFromConfig = (splitsFilterQueryStringFromConfig == null) ? "" : splitsFilterQueryStringFromConfig;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        mSplitsStorage.loadLocal();
        String queryStringFromStorage = mSplitsStorage.getSplitsFilterQueryString();
        if (queryStringFromStorage == null) {
            queryStringFromStorage = "";
        }

        if (mSplitsStorage.getTill() > -1 && mSplitsFilterQueryStringFromConfig.equals(queryStringFromStorage)) {
            return SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS);
        }

        if (!mSplitsFilterQueryStringFromConfig.equals(queryStringFromStorage)) {
            mSplitsStorage.clear();
            mSplitsStorage.updateSplitsFilterQueryString(mSplitsFilterQueryStringFromConfig);
        }

        return SplitTaskExecutionInfo.error(SplitTaskType.LOAD_LOCAL_SPLITS);
    }
}
