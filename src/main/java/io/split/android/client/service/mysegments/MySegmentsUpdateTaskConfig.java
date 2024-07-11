package io.split.android.client.service.mysegments;

import androidx.annotation.NonNull;

import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.telemetry.model.streaming.UpdatesFromSSEEnum;

public class MySegmentsUpdateTaskConfig {

    private static final MySegmentsUpdateTaskConfig MY_SEGMENTS_UPDATE_TASK_CONFIG = new MySegmentsUpdateTaskConfig(SplitTaskType.MY_SEGMENTS_UPDATE,
            SplitInternalEvent.MY_SEGMENTS_UPDATED,
            UpdatesFromSSEEnum.MY_SEGMENTS);
    private static final MySegmentsUpdateTaskConfig MY_LARGE_SEGMENTS_UPDATE_TASK_CONFIG = new MySegmentsUpdateTaskConfig(SplitTaskType.MY_LARGE_SEGMENTS_UPDATE,
            SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED,
            UpdatesFromSSEEnum.MY_LARGE_SEGMENTS);

    private final SplitTaskType mTaskType;
    private final SplitInternalEvent mUpdateEvent;
    private final UpdatesFromSSEEnum mTelemetrySSEKey;

    private MySegmentsUpdateTaskConfig(@NonNull SplitTaskType taskType,
                                       @NonNull SplitInternalEvent updateEvent,
                                       @NonNull UpdatesFromSSEEnum telemetrySSEKey) {
        mTaskType = taskType;
        mUpdateEvent = updateEvent;
        mTelemetrySSEKey = telemetrySSEKey;
    }

    public SplitTaskType getTaskType() {
        return mTaskType;
    }

    public SplitInternalEvent getUpdateEvent() {
        return mUpdateEvent;
    }

    public UpdatesFromSSEEnum getTelemetrySSEKey() {
        return mTelemetrySSEKey;
    }

    @NonNull
    public static MySegmentsUpdateTaskConfig getForMySegments() {
        return MY_SEGMENTS_UPDATE_TASK_CONFIG;
    }

    @NonNull
    public static MySegmentsUpdateTaskConfig getForMyLargeSegments() {
        return MY_LARGE_SEGMENTS_UPDATE_TASK_CONFIG;
    }
}
