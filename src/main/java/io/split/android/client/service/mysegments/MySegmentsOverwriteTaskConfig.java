package io.split.android.client.service.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskType;

public class MySegmentsOverwriteTaskConfig {

    public static final MySegmentsOverwriteTaskConfig MY_SEGMENTS_OVERWRITE_TASK_CONFIG = new MySegmentsOverwriteTaskConfig(
            SplitTaskType.MY_SEGMENTS_OVERWRITE, SplitInternalEvent.MY_SEGMENTS_UPDATED);
    public static final MySegmentsOverwriteTaskConfig MY_LARGE_SEGMENTS_OVERWRITE_TASK_CONFIG = new MySegmentsOverwriteTaskConfig(
            SplitTaskType.MY_LARGE_SEGMENTS_OVERWRITE, SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED);
    private final SplitTaskType mTaskType;
    private final SplitInternalEvent mInternalEvent;

    private MySegmentsOverwriteTaskConfig(@NonNull SplitTaskType taskType, @NonNull SplitInternalEvent internalEvent) {
        mTaskType = checkNotNull(taskType);
        mInternalEvent = checkNotNull(internalEvent);
    }

    @NonNull
    public SplitTaskType getTaskType() {
        return mTaskType;
    }

    @NonNull
    public SplitInternalEvent getInternalEvent() {
        return mInternalEvent;
    }

    public static MySegmentsOverwriteTaskConfig getForMySegments() {
        return MY_SEGMENTS_OVERWRITE_TASK_CONFIG;
    }

    public static MySegmentsOverwriteTaskConfig getForMyLargeSegments() {
        return MY_LARGE_SEGMENTS_OVERWRITE_TASK_CONFIG;
    }
}
