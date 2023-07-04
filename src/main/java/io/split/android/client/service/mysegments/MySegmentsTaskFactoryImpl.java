package io.split.android.client.service.mysegments;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class MySegmentsTaskFactoryImpl implements MySegmentsTaskFactory {

    private final MySegmentsTaskFactoryConfiguration mConfiguration;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public MySegmentsTaskFactoryImpl(@NonNull MySegmentsTaskFactoryConfiguration configuration,
                                     @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        mConfiguration = checkNotNull(configuration);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    public MySegmentsSyncTask createMySegmentsSyncTask(boolean avoidCache) {
        return new MySegmentsSyncTask(mConfiguration.getHttpFetcher(),
                mConfiguration.getStorage(),
                avoidCache,
                mConfiguration.getEventsManager(),
                mTelemetryRuntimeProducer);
    }

    @Override
    public LoadMySegmentsTask createLoadMySegmentsTask() {
        return new LoadMySegmentsTask(mConfiguration.getStorage());
    }

    @Override
    public MySegmentsOverwriteTask createMySegmentsOverwriteTask(List<String> segments) {
        return new MySegmentsOverwriteTask(mConfiguration.getStorage(), segments, mConfiguration.getEventsManager());
    }

    @Override
    public MySegmentsUpdateTask createMySegmentsUpdateTask(boolean add, String segmentName) {
        return new MySegmentsUpdateTask(mConfiguration.getStorage(), add, segmentName, mConfiguration.getEventsManager(), mTelemetryRuntimeProducer);
    }
}
