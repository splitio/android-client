package io.split.android.client.service.telemetry;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.storage.consumer.TelemetryConfigProvider;
import io.split.android.client.utils.Logger;

public class TelemetryConfigRecorderTask implements SplitTask {

    private final HttpRecorder<Config> mTelemetryConfigRecorder;
    private final TelemetryConfigProvider mTelemetryConfigProvider;

    public TelemetryConfigRecorderTask(@NonNull HttpRecorder<Config> telemetryConfigRecorder,
                                       @NonNull TelemetryConfigProvider telemetryConfigProvider) {
        mTelemetryConfigRecorder = checkNotNull(telemetryConfigRecorder);
        mTelemetryConfigProvider = checkNotNull(telemetryConfigProvider);
    }

    @NonNull
    @Override
    public SplitTaskExecutionInfo execute() {
        try {
            mTelemetryConfigRecorder.execute(mTelemetryConfigProvider.getConfigTelemetry());

            return SplitTaskExecutionInfo.success(SplitTaskType.TELEMETRY_CONFIG_TASK);
        } catch (HttpRecorderException e) {
            Logger.e(e);

            return SplitTaskExecutionInfo.error(SplitTaskType.TELEMETRY_CONFIG_TASK);
        }
    }
}
