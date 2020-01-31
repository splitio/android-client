package io.split.android.client.service.splits;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.splits.SplitsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class LoadSplitsTask implements SplitTask {

    private final SplitsStorage mSplitsStorage;

    public LoadSplitsTask(SplitsStorage splitsStorage) {
        mSplitsStorage = checkNotNull(splitsStorage);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
            mSplitsStorage.loadLocal();
        return SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS);
    }
}
