package io.split.android.client.telemetry;

import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.telemetry.model.OperationType;

interface TelemetrySyncTaskExecutionListenerFactory {
    TelemetrySyncTaskExecutionListener create(SplitTaskType taskType, OperationType operationType);
}
