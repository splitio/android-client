package io.split.android.client.service.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

public class LoadMySegmentsTask implements SplitTask {

    private final MySegmentsStorage mMySegmentsStorage;
    private final MySegmentsStorage mMyLargeSegmentsStorage;
    private final SplitTaskType mSplitTaskType;

    public LoadMySegmentsTask(@NonNull MySegmentsStorage mySegmentsStorage,
                              @NonNull MySegmentsStorage myLargeSegmentsStorage,
                              @NonNull LoadMySegmentsTaskConfig config) {
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mMyLargeSegmentsStorage = checkNotNull(myLargeSegmentsStorage);
        mSplitTaskType = config.getTaskType();
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        mMySegmentsStorage.loadLocal();
        mMyLargeSegmentsStorage.loadLocal();
        return SplitTaskExecutionInfo.success(mSplitTaskType);
    }
}
