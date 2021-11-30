package io.split.android.telemetry.storage;

import java.util.List;

import io.split.android.telemetry.model.HTTPErrors;
import io.split.android.telemetry.model.HTTPLatencies;
import io.split.android.telemetry.model.LastSync;
import io.split.android.telemetry.model.StreamingEvent;

public interface TelemetryRuntimeConsumer {

    long getImpressionsStats(int type);

    long getEventsStats(int type);

    LastSync getLastSynchronization();

    HTTPErrors popHTTPErrors();

    HTTPLatencies popHttpLatencies();

    long popAuthRejections();

    long popTokenRefreshes();

    List<StreamingEvent> popStreamingEvents();

    List<String> popTags();

    long getSessionLength();
}
