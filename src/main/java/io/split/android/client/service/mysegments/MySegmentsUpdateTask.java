package io.split.android.client.service.mysegments;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Set;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.utils.logger.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class MySegmentsUpdateTask implements SplitTask {

    private final String mSegmentName;
    private final MySegmentsStorage mMySegmentsStorage;
    private final SplitEventsManager mEventsManager;
    private final boolean mIsAddOperation;

    public MySegmentsUpdateTask(@NonNull MySegmentsStorage mySegmentsStorage,
                                boolean add,
                                @NonNull String segmentName,
                                @NonNull SplitEventsManager eventsManager) {
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mSegmentName = checkNotNull(segmentName);
        mIsAddOperation = add;
        mEventsManager = eventsManager;
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
        } catch (Exception e) {
            logError("Unknown error while adding segment " + mSegmentName + ": " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.MY_SEGMENTS_UPDATE);
        }
        Logger.d("My Segments have been updated. Added " + mSegmentName);
        return SplitTaskExecutionInfo.success(SplitTaskType.MY_SEGMENTS_UPDATE);
    }

    public SplitTaskExecutionInfo remove() {
        try {
            Set<String> segments = mMySegmentsStorage.getAll();
            if(segments.remove(mSegmentName)) {
                updateAndNotify(segments);
            }
        } catch (Exception e) {
            logError("Unknown error while removing segment " + mSegmentName + ": " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.MY_SEGMENTS_UPDATE);
        }
        Logger.d("My Segments have been updated. Removed " + mSegmentName);
        return SplitTaskExecutionInfo.success(SplitTaskType.MY_SEGMENTS_UPDATE);
    }

    private void updateAndNotify(Set<String> segments) {
        mMySegmentsStorage.set(new ArrayList<>(segments));
        mEventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);
    }

    private void logError(String message) {
        Logger.e("Error while executing my segments removal task: " + message);
    }
}
