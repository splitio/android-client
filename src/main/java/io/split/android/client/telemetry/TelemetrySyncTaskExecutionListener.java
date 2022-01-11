package io.split.android.client.telemetry;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class TelemetrySyncTaskExecutionListener implements SplitTaskExecutionListener {

    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final SplitTaskType mTaskType;
    private final OperationType mOperationType;

    public TelemetrySyncTaskExecutionListener(TelemetryRuntimeProducer telemetryRuntimeProducer, SplitTaskType taskType, OperationType operationType) {
        mTelemetryRuntimeProducer = telemetryRuntimeProducer;
        mTaskType = taskType;
        mOperationType = operationType;
    }

    @Override
    public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
        if (!mTaskType.equals(taskInfo.getTaskType())) {
            return;
        }

        if (SplitTaskExecutionStatus.SUCCESS.equals(taskInfo.getStatus())) {
            mTelemetryRuntimeProducer.recordSuccessfulSync(mOperationType, System.currentTimeMillis());
        } else if (SplitTaskExecutionStatus.ERROR.equals(taskInfo.getStatus())) {
            mTelemetryRuntimeProducer.recordSyncError(mOperationType, taskInfo.getIntegerValue(SplitTaskExecutionInfo.HTTP_STATUS));
        }
    }
}
