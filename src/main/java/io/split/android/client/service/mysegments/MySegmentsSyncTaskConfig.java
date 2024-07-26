package io.split.android.client.service.mysegments;

import androidx.annotation.NonNull;

import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.telemetry.model.OperationType;

public class MySegmentsSyncTaskConfig {

    private static final MySegmentsSyncTaskConfig MY_SEGMENTS_TASK_CONFIG = new MySegmentsSyncTaskConfig(
            SplitTaskType.MY_SEGMENTS_SYNC,
            SplitInternalEvent.MY_SEGMENTS_UPDATED,
            SplitInternalEvent.MY_SEGMENTS_FETCHED,
            OperationType.MY_SEGMENT);
    private static final MySegmentsSyncTaskConfig MY_LARGE_SEGMENTS_TASK_CONFIG = new MySegmentsSyncTaskConfig(
            SplitTaskType.MY_LARGE_SEGMENT_SYNC,
            SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED,
            SplitInternalEvent.MY_LARGE_SEGMENTS_FETCHED,
            OperationType.MY_LARGE_SEGMENT);
    private final SplitTaskType mTaskType;
    private final SplitInternalEvent mUpdateEvent;
    private final SplitInternalEvent mFetchedEvent;
    private final OperationType mTelemetryOperationType;

    private MySegmentsSyncTaskConfig(@NonNull SplitTaskType taskType,
                                     @NonNull SplitInternalEvent updateEvent,
                                     @NonNull SplitInternalEvent fetchedEvent,
                                     @NonNull OperationType telemetryOperationType) {
        mTaskType = taskType;
        mUpdateEvent = updateEvent;
        mFetchedEvent = fetchedEvent;
        mTelemetryOperationType = telemetryOperationType;
    }

    SplitTaskType getTaskType() {
        return mTaskType;
    }

    SplitInternalEvent getUpdateEvent() {
        return mUpdateEvent;
    }

    SplitInternalEvent getFetchedEvent() {
        return mFetchedEvent;
    }

    OperationType getTelemetryOperationType() {
        return mTelemetryOperationType;
    }

    @NonNull
    public static MySegmentsSyncTaskConfig getForMySegments() {
        return MY_SEGMENTS_TASK_CONFIG;
    }

    @NonNull
    public static MySegmentsSyncTaskConfig getForMyLargeSegments() {
        return MY_LARGE_SEGMENTS_TASK_CONFIG;
    }
}
