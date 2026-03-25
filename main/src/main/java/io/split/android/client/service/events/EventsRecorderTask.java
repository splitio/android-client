package io.split.android.client.service.events;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.service.HttpRecorderSubmitterAdapter;
import io.split.android.client.service.TelemetryRecorderAdapter;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.submitter.RecorderTask;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class EventsRecorderTask extends RecorderTask<Event, List<Event>> {

    public static final int FAILING_CHUNK_SIZE = 20;

    public EventsRecorderTask(@NonNull HttpRecorder<List<Event>> httpRecorder,
                              @NonNull PersistentEventsStorage storage,
                              @NonNull EventsRecorderTaskConfig config,
                              @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        super(storage,
                new HttpRecorderSubmitterAdapter<>(httpRecorder),
                config.getEventsPerPush(),
                SplitTaskType.EVENTS_RECORDER,
                new TelemetryRecorderAdapter(telemetryRuntimeProducer, OperationType.EVENTS),
                FAILING_CHUNK_SIZE);
    }

    @Override
    protected List<Event> transformForSubmission(List<Event> items) {
        return items;
    }

    @Override
    protected long estimateItemSize(Event item) {
        return item.getSizeInBytes();
    }
}
