package io.split.android.client.service.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

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
                mTelemetryRuntimeProducer,
                mConfiguration.getMySegmentsSyncTaskConfig());
    }

    @Override
    public LoadMySegmentsTask createLoadMySegmentsTask() {
        return new LoadMySegmentsTask(mConfiguration.getStorage(), mConfiguration.getLoadMySegmentsTaskConfig());
    }

    @Override
    public MySegmentsOverwriteTask createMySegmentsOverwriteTask(List<String> segments) {
        return new MySegmentsOverwriteTask(mConfiguration.getStorage(), segments, mConfiguration.getEventsManager(), mConfiguration.getMySegmentsOverwriteTaskConfig());
    }

    @Override
    public MySegmentsUpdateTask createMySegmentsUpdateTask(boolean add, Set<String> segmentNames) {
        return new MySegmentsUpdateTask(mConfiguration.getStorage(), add, segmentNames, mConfiguration.getEventsManager(), mTelemetryRuntimeProducer, mConfiguration.getMySegmentsUpdateTaskConfig());
    }
}
