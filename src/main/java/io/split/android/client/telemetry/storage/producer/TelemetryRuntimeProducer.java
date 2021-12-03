package io.split.android.client.telemetry.storage.producer;

import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;

public interface TelemetryRuntimeProducer {

    void addTag(String tag);

    void recordImpressionStats(ImpressionsDataType dataType, long count);

    void recordEventStats(EventsDataRecordsEnum dataType, long count);

    void recordSuccessfulSync(OperationType resource, long time);

    void recordSyncError(OperationType syncedResource, int status);

    void recordSyncLatency(OperationType resource, long latency);

    void recordAuthRejections();

    void recordTokenRefreshes();

    void recordStreamingEvents(StreamingEvent streamingEvent);

    void recordSessionLength(long sessionLength);
}
