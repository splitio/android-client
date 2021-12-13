package io.split.android.client.telemetry.storage;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.telemetry.model.Config;

public class TelemetryConfigProviderImpl implements TelemetryConfigProvider {

    private final TelemetryStorageConsumer mTelemetryConsumer;
    private final SplitClientConfig mSplitClientConfig;

    public TelemetryConfigProviderImpl(@NonNull TelemetryStorageConsumer telemetryConsumer,
                                       @NonNull SplitClientConfig splitClientConfig) {
        mTelemetryConsumer = checkNotNull(telemetryConsumer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
    }

    @Override
    public Config getConfigTelemetry() {
        // TODO implement
        return new Config();
    }
}
