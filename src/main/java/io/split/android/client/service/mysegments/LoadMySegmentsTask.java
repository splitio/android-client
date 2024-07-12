package io.split.android.client.service.mysegments;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

import static io.split.android.client.utils.Utils.checkNotNull;

public class LoadMySegmentsTask implements SplitTask {

    private final MySegmentsStorage mMySegmentsStorage;
    private final SplitTaskType mSplitTaskType;

    public LoadMySegmentsTask(@NonNull MySegmentsStorage mySegmentsStorage, @NonNull LoadMySegmentsTaskConfig config) {
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mSplitTaskType = config.getTaskType();
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        mMySegmentsStorage.loadLocal();
        return SplitTaskExecutionInfo.success(mSplitTaskType);
    }
}
