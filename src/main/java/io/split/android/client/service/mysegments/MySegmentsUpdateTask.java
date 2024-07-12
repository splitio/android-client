package io.split.android.client.service.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Set;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.telemetry.model.streaming.UpdatesFromSSEEnum;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.logger.Logger;

public class MySegmentsUpdateTask implements SplitTask {

    private final String mSegmentName;
    private final MySegmentsStorage mMySegmentsStorage;
    private final SplitEventsManager mEventsManager;
    private final boolean mIsAddOperation;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    private final SplitTaskType mTaskType;
    private final SplitInternalEvent mUpdateEvent;
    private final UpdatesFromSSEEnum mTelemetrySSEKey;

    public MySegmentsUpdateTask(@NonNull MySegmentsStorage mySegmentsStorage,
                                boolean add,
                                @NonNull String segmentName,
                                @NonNull SplitEventsManager eventsManager,
                                @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer,
                                @NonNull MySegmentsUpdateTaskConfig config) {
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mSegmentName = checkNotNull(segmentName);
        mIsAddOperation = add;
        mEventsManager = checkNotNull(eventsManager);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
        mTaskType = config.getTaskType();
        mUpdateEvent = config.getUpdateEvent();
        mTelemetrySSEKey = config.getTelemetrySSEKey();
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        if (mIsAddOperation) {
            return add();
        }
        return remove();
    }

    private SplitTaskExecutionInfo add() {
        try {
            Set<String> segments = mMySegmentsStorage.getAll();
            if (!segments.contains(mSegmentName)) {
                segments.add(mSegmentName);
                updateAndNotify(segments);
            }
            mTelemetryRuntimeProducer.recordUpdatesFromSSE(mTelemetrySSEKey);
        } catch (Exception e) {
            logError("Unknown error while adding segment " + mSegmentName + ": " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(mTaskType);
        }
        Logger.d("My Segments have been updated. Added " + mSegmentName);
        return SplitTaskExecutionInfo.success(mTaskType);
    }

    public SplitTaskExecutionInfo remove() {
        try {
            Set<String> segments = mMySegmentsStorage.getAll();
            if(segments.remove(mSegmentName)) {
                updateAndNotify(segments);
            }
            mTelemetryRuntimeProducer.recordUpdatesFromSSE(mTelemetrySSEKey);
        } catch (Exception e) {
            logError("Unknown error while removing segment " + mSegmentName + ": " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(mTaskType);
        }
        Logger.d("My Segments have been updated. Removed " + mSegmentName);
        return SplitTaskExecutionInfo.success(mTaskType);
    }

    private void updateAndNotify(Set<String> segments) {
        mMySegmentsStorage.set(new ArrayList<>(segments));
        mEventsManager.notifyInternalEvent(mUpdateEvent);
    }

    private void logError(String message) {
        Logger.e("Error while executing my segments removal task: " + message);
    }
}
