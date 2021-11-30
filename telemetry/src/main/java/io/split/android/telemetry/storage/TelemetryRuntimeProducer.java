package io.split.android.telemetry.storage;

import io.split.android.telemetry.model.EventsDataRecordsEnum;
import io.split.android.telemetry.model.HTTPLatenciesType;
import io.split.android.telemetry.model.ImpressionsDataType;
import io.split.android.telemetry.model.LastSynchronizationRecords;
import io.split.android.telemetry.model.Resource;
import io.split.android.telemetry.model.StreamingEvent;

public interface TelemetryRuntimeProducer {

    void addTag(String tag);

    void recordImpressionStats(ImpressionsDataType dataType, long count);

    void recordEventStats(EventsDataRecordsEnum dataType, long count);

    void recordSuccessfulSync(LastSynchronizationRecords resource, long time);

    void recordSyncError(Resource resource, int status);

    void recordSyncLatency(HTTPLatenciesType resource, long latency);

    void recordAuthRejections();

    void recordTokenRefreshes();

    void recordStreamingEvents(StreamingEvent streamingEvent);

    void recordSessionLength(long sessionLength);
}
