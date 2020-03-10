package io.split.android.client.service.executor;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitTaskExecutionInfo {

    public static final String NON_SENT_RECORDS = "NON_SENT_RECORDS";
    public static final String NON_SENT_BYTES = "NON_SENT_BYTES";

    final private SplitTaskType taskType;
    final private SplitTaskExecutionStatus status;
    final private Map<String, Object> data;
//    final private int nonSentRecords;
//    final private long nonSentBytes;

    public static SplitTaskExecutionInfo success(SplitTaskType taskType) {
        return new SplitTaskExecutionInfo(
                taskType, SplitTaskExecutionStatus.SUCCESS, new HashMap<>());
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
        this.taskType = taskType;
        this.status = checkNotNull(status);
        this.data = checkNotNull(data);
    }

    public SplitTaskExecutionStatus getStatus() {
        return status;
    }

    public SplitTaskType getTaskType() {
        return taskType;
    }

    public int getIntegerValue(String paramName) {
        Object value = data.get(paramName);
        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    public long getLongValue(String paramName) {
        Object value = data.get(paramName);
        return value == null ? 0 : Long.parseLong(value.toString());
    }
}
