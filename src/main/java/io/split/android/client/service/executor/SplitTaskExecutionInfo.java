package io.split.android.client.service.executor;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitTaskExecutionInfo {

    public static final String NON_SENT_RECORDS = "NON_SENT_RECORDS";
    public static final String NON_SENT_BYTES = "NON_SENT_BYTES";
    public static final String IS_STREAMING_ENABLED = "IS_STREAMING_ENABLED";
    public static final String IS_VALID_API_KEY = "IS_VALID_API_KEY";
    public static final String PARSED_SSE_JWT = "PARSED_SSE_JWT";
    public static final String UNEXPECTED_ERROR = "UNEXPECTED_ERROR";

    final private SplitTaskType taskType;
    final private SplitTaskExecutionStatus status;
    final private Map<String, Object> data;

    public static SplitTaskExecutionInfo success(SplitTaskType taskType) {
        return new SplitTaskExecutionInfo(
                taskType, SplitTaskExecutionStatus.SUCCESS, new HashMap<>());
    }

    public static SplitTaskExecutionInfo success(SplitTaskType taskType,
                                                 Map<String, Object> data) {
        return new SplitTaskExecutionInfo(
                taskType, SplitTaskExecutionStatus.SUCCESS, data);
    }

    public static SplitTaskExecutionInfo error(SplitTaskType taskType) {
        return new SplitTaskExecutionInfo(
                taskType, SplitTaskExecutionStatus.ERROR, new HashMap<>());
    }

    public static SplitTaskExecutionInfo error(SplitTaskType taskType,
                                               Map<String, Object> data) {
        return new SplitTaskExecutionInfo(
                taskType, SplitTaskExecutionStatus.ERROR, data);
    }

    private SplitTaskExecutionInfo(SplitTaskType taskType,
                                   @NonNull SplitTaskExecutionStatus status,
                                   @NonNull Map<String, Object> data) {
        this.taskType = checkNotNull(taskType);
        this.status = checkNotNull(status);
        this.data = checkNotNull(data);
    }

    public SplitTaskExecutionStatus getStatus() {
        return status;
    }

    public SplitTaskType getTaskType() {
        return taskType;
    }

    public @Nullable Integer getIntegerValue(String paramName) {
        Object value = data.get(paramName);
        return value != null ? Integer.parseInt(value.toString()) : null;
    }

    public @Nullable Long getLongValue(String paramName) {
        Object value = data.get(paramName);
        return value != null ? Long.parseLong(value.toString()) : null;
    }

    public @Nullable String getStringValue(String paramName) {
        Object value = data.get(paramName);
        return value != null ? value.toString() : null;
    }

    public @Nullable Boolean getBoolValue(String paramName) {
        Object value = data.get(paramName);
        return value != null ? Boolean.parseBoolean(value.toString()) : null;
    }

    public @Nullable Object getObjectValue(String paramName) {
        return data.get(paramName);
    }
}
