package io.split.android.client.service.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class MySegmentsTaskFactoryProviderImpl implements MySegmentsTaskFactoryProvider {

    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public MySegmentsTaskFactoryProviderImpl(@NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    public MySegmentsTaskFactory getFactory(MySegmentsTaskFactoryConfiguration configuration) {
        return new MySegmentsTaskFactoryImpl(configuration, mTelemetryRuntimeProducer);
    }
}
