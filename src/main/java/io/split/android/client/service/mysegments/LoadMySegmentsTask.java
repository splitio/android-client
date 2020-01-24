package io.split.android.client.service.mysegments;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

public class LoadMySegmentsTask implements SplitTask {

    private final MySegmentsStorage mMySegmentsStorage;

    public LoadMySegmentsTask(@NonNull MySegmentsStorage mySegmentsStorage) {

        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        mMySegmentsStorage.loadLocal();
        return SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_MY_SYGMENTS);
    }
}
