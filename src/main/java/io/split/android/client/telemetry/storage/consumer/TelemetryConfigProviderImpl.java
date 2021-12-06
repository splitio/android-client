package io.split.android.client.telemetry.storage.consumer;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.telemetry.model.Config;

public class TelemetryConfigProviderImpl implements TelemetryConfigProvider {

    private final TelemetryConsumer mTelemetryConsumer;
    private final SplitClientConfig mSplitClientConfig;

    public TelemetryConfigProviderImpl(@NonNull TelemetryConsumer telemetryConsumer,
                                       @NonNull SplitClientConfig splitClientConfig) {
        mTelemetryConsumer = checkNotNull(telemetryConsumer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
    }

    @Override
    public Config getConfigTelemetry() {
        //TODO implement
        return new Config();
    }
}
