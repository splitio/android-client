package io.split.android.client.service.events;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Event;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventsRecorderTask implements SplitTask {
    public final static int FAILING_CHUNK_SIZE = 20;
    private final PersistentEventsStorage mPersistenEventsStorage;
    private final HttpRecorder<List<Event>> mHttpRecorder;
    private final EventsRecorderTaskConfig mConfig;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public EventsRecorderTask(@NonNull HttpRecorder<List<Event>> httpRecorder,
                              @NonNull PersistentEventsStorage persistenEventsStorage,
                              @NonNull EventsRecorderTaskConfig config,
                              @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        mHttpRecorder = checkNotNull(httpRecorder);
        mPersistenEventsStorage = checkNotNull(persistenEventsStorage);
        mConfig = checkNotNull(config);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        SplitTaskExecutionStatus status = SplitTaskExecutionStatus.SUCCESS;
        int nonSentRecords = 0;
        long nonSentBytes = 0;
        List<Event> events;
        List<Event> failingEvents = new ArrayList<>();
        do {
            events = mPersistenEventsStorage.pop(mConfig.getEventsPerPush());
            if (events.size() > 0) {
                long startTime = System.currentTimeMillis();
                long latency = 0;
                try {
                    Logger.d("Posting %d Split events", events.size());
                    mHttpRecorder.execute(events);

                    long now = System.currentTimeMillis();
                    latency = now - startTime;
                    mTelemetryRuntimeProducer.recordSuccessfulSync(OperationType.EVENTS, now);

                    mPersistenEventsStorage.delete(events);
                    Logger.d("%d split events sent", events.size());
                } catch (HttpRecorderException e) {
                    status = SplitTaskExecutionStatus.ERROR;
                    nonSentRecords += mConfig.getEventsPerPush();
                    nonSentBytes += sumEventBytes(events);
                    Logger.e("Event recorder task: Some events couldn't be sent" +
                            "Saving to send them in a new iteration: " +
                            e.getLocalizedMessage());
                    e.printStackTrace();
                    failingEvents.addAll(events);

                    mTelemetryRuntimeProducer.recordSyncError(OperationType.EVENTS, e.getHttpStatus());
                } finally {
                    mTelemetryRuntimeProducer.recordSyncLatency(OperationType.EVENTS, latency);
                }
            }
        } while (events.size() == mConfig.getEventsPerPush());

        // Update events by chunks to avoid sqlite errors
        List<List<Event>> failingChunks = Lists.partition(failingEvents, FAILING_CHUNK_SIZE);
        for(List<Event> chunk : failingChunks) {
            mPersistenEventsStorage.setActive(chunk);
        }

        if (status == SplitTaskExecutionStatus.ERROR) {
            Map<String, Object> data = new HashMap<>();
            data.put(SplitTaskExecutionInfo.NON_SENT_RECORDS, nonSentRecords);
            data.put(SplitTaskExecutionInfo.NON_SENT_BYTES, nonSentBytes);

            return SplitTaskExecutionInfo.error(
                    SplitTaskType.EVENTS_RECORDER, data);
        }
        return SplitTaskExecutionInfo.success(SplitTaskType.EVENTS_RECORDER);
    }

    private long sumEventBytes(List<Event> events) {
        long totalBytes = 0;
        for (Event event : events) {
            totalBytes += event.getSizeInBytes();
        }
        return totalBytes;
    }
}
