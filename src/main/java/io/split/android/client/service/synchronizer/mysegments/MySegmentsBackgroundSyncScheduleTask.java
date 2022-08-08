package io.split.android.client.service.synchronizer.mysegments;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.Set;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;

public class MySegmentsBackgroundSyncScheduleTask implements SplitTask {

    private final MySegmentsWorkManagerWrapper mWorkManagerWrapper;
    private final Set<String> mKeySet;

    public MySegmentsBackgroundSyncScheduleTask(@NonNull MySegmentsWorkManagerWrapper workManagerWrapper,
                                                @NonNull Set<String> keySet) {
        mWorkManagerWrapper = checkNotNull(workManagerWrapper);
        mKeySet = checkNotNull(keySet);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        mWorkManagerWrapper.scheduleMySegmentsWork(mKeySet);

        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }
}
