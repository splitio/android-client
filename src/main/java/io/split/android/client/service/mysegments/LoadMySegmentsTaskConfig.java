package io.split.android.client.service.mysegments;

import io.split.android.client.service.executor.SplitTaskType;

public class LoadMySegmentsTaskConfig {

    private static final LoadMySegmentsTaskConfig LOAD_MY_SEGMENTS_TASK_CONFIG = new LoadMySegmentsTaskConfig(SplitTaskType.LOAD_LOCAL_MY_SYGMENTS);
    private static final LoadMySegmentsTaskConfig LOAD_MY_LARGE_SEGMENTS_TASK_CONFIG = new LoadMySegmentsTaskConfig(SplitTaskType.LOAD_LOCAL_MY_LARGE_SEGMENTS);

    private final SplitTaskType mTaskType;

    private LoadMySegmentsTaskConfig(SplitTaskType taskType) {
        mTaskType = taskType;
    }

    public SplitTaskType getTaskType() {
        return mTaskType;
    }

    public static LoadMySegmentsTaskConfig getForMySegments() {
        return LOAD_MY_SEGMENTS_TASK_CONFIG;
    }

    public static LoadMySegmentsTaskConfig getForMyLargeSegments() {
        return LOAD_MY_LARGE_SEGMENTS_TASK_CONFIG;
    }
}
