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
                mConfiguration.getMySegmentsStorage(),
                avoidCache,
                mConfiguration.getEventsManager(),
                mTelemetryRuntimeProducer,
                mConfiguration.getMySegmentsSyncTaskConfig());
    }

    @Override
    public LoadMySegmentsTask createLoadMySegmentsTask() {
        return new LoadMySegmentsTask(mConfiguration.getMySegmentsStorage(), mConfiguration.getMyLargeSegmentsStorage(), mConfiguration.getLoadMySegmentsTaskConfig());
    }

    @Override
    public MySegmentsOverwriteTask createMySegmentsOverwriteTask(List<String> segments, Long changeNumber) {
        return new MySegmentsOverwriteTask(mConfiguration.getMySegmentsStorage(), segments, changeNumber, mConfiguration.getEventsManager(), mConfiguration.getMySegmentsOverwriteTaskConfig());
    }

    @Override
    public MySegmentsUpdateTask createMySegmentsUpdateTask(boolean add, Set<String> segmentNames, Long changeNumber) {
        return new MySegmentsUpdateTask(mConfiguration.getMySegmentsStorage(), add, segmentNames, changeNumber, mConfiguration.getEventsManager(), mTelemetryRuntimeProducer, mConfiguration.getMySegmentsUpdateTaskConfig());
    }

    @Override
    public MySegmentsUpdateTask createMyLargeSegmentsUpdateTask(boolean add, Set<String> segmentNames, Long changeNumber) {
        return new MySegmentsUpdateTask(mConfiguration.getMyLargeSegmentsStorage(), add, segmentNames, changeNumber, mConfiguration.getEventsManager(), mTelemetryRuntimeProducer, mConfiguration.getMyLargeSegmentsUpdateTaskConfig());
    }
}
