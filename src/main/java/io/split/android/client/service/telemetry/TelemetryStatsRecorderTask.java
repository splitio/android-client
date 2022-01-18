package io.split.android.client.service.telemetry;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.Stats;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.telemetry.storage.TelemetryStatsProvider;
import io.split.android.client.utils.Logger;

public class TelemetryStatsRecorderTask implements SplitTask {

    private final HttpRecorder<Stats> mTelemetryStatsRecorder;
    private final TelemetryStatsProvider mTelemetryStatsProvider;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public TelemetryStatsRecorderTask(@NonNull HttpRecorder<Stats> telemetryStatsRecorder,
                                      @NonNull TelemetryStatsProvider telemetryStatsProvider,
                                      @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        mTelemetryStatsRecorder = checkNotNull(telemetryStatsRecorder);
        mTelemetryStatsProvider = checkNotNull(telemetryStatsProvider);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        long startTime = System.currentTimeMillis();
        try {
            Stats pendingStats = mTelemetryStatsProvider.getTelemetryStats();

            mTelemetryStatsRecorder.execute(pendingStats);

            mTelemetryStatsProvider.clearStats();

            return SplitTaskExecutionInfo.success(SplitTaskType.TELEMETRY_STATS_TASK);
        } catch (HttpRecorderException e) {
            Logger.e(e);
            mTelemetryRuntimeProducer.recordSyncError(OperationType.TELEMETRY, e.getHttpStatus());

            return SplitTaskExecutionInfo.error(SplitTaskType.TELEMETRY_STATS_TASK);
        } finally {
            mTelemetryRuntimeProducer.recordSyncLatency(OperationType.TELEMETRY, System.currentTimeMillis() - startTime);
        }
    }
}
