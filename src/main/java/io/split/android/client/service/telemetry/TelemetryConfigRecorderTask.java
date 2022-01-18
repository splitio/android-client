package io.split.android.client.service.telemetry;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.Collections;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryConfigProvider;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.Logger;

public class TelemetryConfigRecorderTask implements SplitTask {

    private final HttpRecorder<Config> mTelemetryConfigRecorder;
    private final TelemetryConfigProvider mTelemetryConfigProvider;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public TelemetryConfigRecorderTask(@NonNull HttpRecorder<Config> telemetryConfigRecorder,
                                       @NonNull TelemetryConfigProvider telemetryConfigProvider,
                                       @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        mTelemetryConfigRecorder = checkNotNull(telemetryConfigRecorder);
        mTelemetryConfigProvider = checkNotNull(telemetryConfigProvider);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        long startTime = System.currentTimeMillis();
        try {
            mTelemetryConfigRecorder.execute(mTelemetryConfigProvider.getConfigTelemetry());

            return SplitTaskExecutionInfo.success(SplitTaskType.TELEMETRY_CONFIG_TASK);
        } catch (HttpRecorderException e) {
            Logger.e(e);
            return SplitTaskExecutionInfo.error(SplitTaskType.TELEMETRY_CONFIG_TASK, Collections.singletonMap(SplitTaskExecutionInfo.HTTP_STATUS, e.getHttpStatus()));
        } finally {
            mTelemetryRuntimeProducer.recordSyncLatency(OperationType.TELEMETRY, System.currentTimeMillis() - startTime);
        }
    }
}
