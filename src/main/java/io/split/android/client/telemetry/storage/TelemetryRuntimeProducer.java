package io.split.android.client.telemetry.storage;

import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.HTTPLatenciesType;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.LastSynchronizationRecords;
import io.split.android.client.telemetry.model.SyncedResource;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;

public interface TelemetryRuntimeProducer {

    void addTag(String tag);

    void recordImpressionStats(ImpressionsDataType dataType, long count);

    void recordEventStats(EventsDataRecordsEnum dataType, long count);

    void recordSuccessfulSync(LastSynchronizationRecords resource, long time);

    void recordSyncError(SyncedResource syncedResource, int status);

    void recordSyncLatency(HTTPLatenciesType resource, long latency);

    void recordAuthRejections();

    void recordTokenRefreshes();

    void recordStreamingEvents(StreamingEvent streamingEvent);

    void recordSessionLength(long sessionLength);
}
