package io.split.android.client.service.executor;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitTaskExecutionInfo {
    final private SplitTaskType taskType;
    final private SplitTaskExecutionStatus status;
    final private int nonSentRecords;
    final private long nonSentBytes;

    public static SplitTaskExecutionInfo success(SplitTaskType taskType) {
        return new SplitTaskExecutionInfo(
                taskType, SplitTaskExecutionStatus.SUCCESS,
                0, 0);
    }

    public static SplitTaskExecutionInfo error(SplitTaskType taskType) {
        return new SplitTaskExecutionInfo(
                taskType, SplitTaskExecutionStatus.ERROR,
                0, 0);
    }

    public static SplitTaskExecutionInfo error(SplitTaskType taskType,
                                               int nonSentRecords,
                                               long nonSentBytes) {
        return new SplitTaskExecutionInfo(
                taskType, SplitTaskExecutionStatus.ERROR,
                nonSentRecords, nonSentBytes);
    }

    private SplitTaskExecutionInfo(SplitTaskType taskType,
                                  @NonNull SplitTaskExecutionStatus status,
                                  int nonSentRecords,
                                  long nonSentBytes){
        this.taskType = taskType;
        this.status = checkNotNull(status);
        this.nonSentRecords = nonSentRecords;
        this.nonSentBytes = nonSentBytes;
    }

    public SplitTaskExecutionStatus getStatus() {
        return status;
    }

    public SplitTaskType getTaskType() {
        return taskType;
    }

    public int getNonSentRecords() {
        return nonSentRecords;
    }

    public long getNonSentBytes() {
        return nonSentBytes;
    }
}
