package io.split.android.client.telemetry.storage;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.Stats;

public class TelemetryStatsProviderImpl implements TelemetryStatsProvider {

    private final TelemetryStorageConsumer mTelemetryStorageConsumer;
    private final SplitsStorage mSplitsStorage;
    private final MySegmentsStorage mMySegmentsStorage;
    private Stats pendingStats = null;

    public TelemetryStatsProviderImpl(@NonNull TelemetryStorageConsumer telemetryStorageConsumer,
                                      @NonNull SplitsStorage splitsStorage,
                                      @NonNull MySegmentsStorage mySegmentsStorage) {
        mTelemetryStorageConsumer = checkNotNull(telemetryStorageConsumer);
        mSplitsStorage = checkNotNull(splitsStorage);
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
    }

    @Override
    public Stats getTelemetryStats() {
        if (pendingStats == null) {
            pendingStats = buildStats();
        }

        return pendingStats;
    }

    @Override
    public void clearStats() {
        pendingStats = null;
    }

    private Stats buildStats() {
        Stats stats = new Stats();

        stats.setStreamingEvents(mTelemetryStorageConsumer.popStreamingEvents());
        stats.setSplitCount(mSplitsStorage.getAll().size());
        stats.setTags(mTelemetryStorageConsumer.popTags());
        stats.setMethodLatencies(mTelemetryStorageConsumer.popLatencies());
        stats.setSegmentCount(mMySegmentsStorage.getAll().size());
        stats.setSessionLengthMs(mTelemetryStorageConsumer.getSessionLength());
        stats.setLastSynchronizations(mTelemetryStorageConsumer.getLastSynchronization());
        stats.setImpressionsDropped(mTelemetryStorageConsumer.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DROPPED));
        stats.setImpressionsQueued(mTelemetryStorageConsumer.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_QUEUED));
        stats.setImpressionsDeduped(mTelemetryStorageConsumer.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DEDUPED));
        stats.setMethodExceptions(mTelemetryStorageConsumer.popExceptions());
        stats.setHttpLatencies(mTelemetryStorageConsumer.popHttpLatencies());
        stats.setHttpErrors(mTelemetryStorageConsumer.popHttpErrors());
        stats.setTokenRefreshes(mTelemetryStorageConsumer.popTokenRefreshes());
        stats.setAuthRejections(mTelemetryStorageConsumer.popAuthRejections());
        stats.setEventsQueued(mTelemetryStorageConsumer.getEventsStats(EventsDataRecordsEnum.EVENTS_QUEUED));
        stats.setEventsQueued(mTelemetryStorageConsumer.getEventsStats(EventsDataRecordsEnum.EVENTS_DROPPED));

        return stats;
    }
}
