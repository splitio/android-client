package io.split.android.client.telemetry.storage.consumer;

import java.util.List;

import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.HTTPErrors;
import io.split.android.client.telemetry.model.HTTPLatencies;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.LastSync;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;

public interface TelemetryRuntimeConsumer {

    long getImpressionsStats(ImpressionsDataType type);

    long getEventsStats(EventsDataRecordsEnum type);

    LastSync getLastSynchronization();

    HTTPErrors popHTTPErrors();

    HTTPLatencies popHttpLatencies();

    long popAuthRejections();

    long popTokenRefreshes();

    List<StreamingEvent> popStreamingEvents();

    List<String> popTags();

    long getSessionLength();
}
