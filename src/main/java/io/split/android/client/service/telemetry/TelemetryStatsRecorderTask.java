package io.split.android.client.service.telemetry;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.telemetry.model.Stats;
import io.split.android.client.telemetry.storage.TelemetryStatsProvider;

public class TelemetryStatsRecorderTask implements SplitTask {

    private final HttpRecorder<Stats> mTelemetryStatsRecorder;
    private final TelemetryStatsProvider mTelemetryStatsProvider;

    public TelemetryStatsRecorderTask(@NonNull HttpRecorder<Stats> telemetryStatsRecorder,
                                      @NonNull TelemetryStatsProvider telemetryStatsProvider) {
        mTelemetryStatsRecorder = checkNotNull(telemetryStatsRecorder);
        mTelemetryStatsProvider = checkNotNull(telemetryStatsProvider);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            Stats pendingStats = mTelemetryStatsProvider.getTelemetryStats();

            mTelemetryStatsRecorder.execute(pendingStats);

            mTelemetryStatsProvider.clearStats();

            return SplitTaskExecutionInfo.success(SplitTaskType.TELEMETRY_STATS_TASK);
        } catch (Exception e) {

            return SplitTaskExecutionInfo.error(SplitTaskType.TELEMETRY_STATS_TASK);
        }
    }
}
