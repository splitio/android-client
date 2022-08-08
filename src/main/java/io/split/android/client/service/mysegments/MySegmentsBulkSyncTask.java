package io.split.android.client.service.mysegments;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.Set;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;

public class MySegmentsBulkSyncTask implements SplitTask {

    private final Set<MySegmentsSyncTask> mMySegmentsSyncTasks;

    public MySegmentsBulkSyncTask(@NonNull Set<MySegmentsSyncTask> mySegmentsSyncTasks) {
        mMySegmentsSyncTasks = checkNotNull(mySegmentsSyncTasks);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        for (SplitTask task : mMySegmentsSyncTasks) {
            task.execute();
        }

        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }
}
