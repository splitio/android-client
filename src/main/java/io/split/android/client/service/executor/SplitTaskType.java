package io.split.android.client.service.executor;

public enum SplitTaskType {
    SPLITS_SYNC, MY_SEGMENTS_SYNC, EVENTS_RECORDER, IMPRESSIONS_RECORDER,
    LOAD_LOCAL_SPLITS, LOAD_LOCAL_MY_SYGMENTS, SSE_AUTHENTICATION_TASK,
    MY_SEGMENTS_OVERWRITE, SPLIT_KILL, FILTER_SPLITS_CACHE, GENERIC_TASK,
    CLEAN_UP_DATABASE, IMPRESSIONS_COUNT_RECORDER, SAVE_IMPRESSIONS_COUNT,
    MY_SEGMENTS_UPDATE, LOAD_LOCAL_ATTRIBUTES,
    TELEMETRY_CONFIG_TASK, TELEMETRY_STATS_TASK,
    SAVE_UNIQUE_KEYS_TASK, UNIQUE_KEYS_RECORDER_TASK,
}
