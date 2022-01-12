package io.split.android.client.telemetry.model;

import androidx.annotation.Nullable;

import io.split.android.client.service.executor.SplitTaskType;

public enum OperationType {
    SPLITS,
    IMPRESSIONS,
    IMPRESSIONS_COUNT,
    EVENTS,
    TELEMETRY,
    TOKEN,
    MY_SEGMENT;

    @Nullable
    public static OperationType getFromTaskType(SplitTaskType taskType) {
        switch (taskType) {
            case SPLITS_SYNC:
                return OperationType.SPLITS;
            case MY_SEGMENTS_SYNC:
                return OperationType.MY_SEGMENT;
            case TELEMETRY_STATS_TASK:
                return OperationType.TELEMETRY;
            case EVENTS_RECORDER:
                return OperationType.EVENTS;
            case IMPRESSIONS_RECORDER:
                return OperationType.IMPRESSIONS;
            case IMPRESSIONS_COUNT_RECORDER:
                return OperationType.IMPRESSIONS_COUNT;
        }

        return null;
    }
}
