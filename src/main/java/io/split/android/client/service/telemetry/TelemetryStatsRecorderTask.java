package io.split.android.client.service.telemetry;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;

// TODO: implement
public class TelemetryStatsRecorderTask implements SplitTask {

    public TelemetryStatsRecorderTask() {

    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        // TODO
        return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
    }
}
