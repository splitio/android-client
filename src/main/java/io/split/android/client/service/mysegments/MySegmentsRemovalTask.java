package io.split.android.client.service.mysegments;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.synchronizer.MySegmentsChangeChecker;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class MySegmentsRemovalTask implements SplitTask {

    private final String mSegmentName;
    private final MySegmentsStorage mMySegmentsStorage;
    private final SplitEventsManager mEventsManager;

    public MySegmentsRemovalTask(@NonNull MySegmentsStorage mySegmentsStorage,
                                 @NonNull String segmentName,
                                 @NonNull SplitEventsManager eventsManager) {
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mSegmentName = segmentName;
        mEventsManager = eventsManager;
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        try {
            if (mSegmentName == null) {
                logError("Segment to remove could not be null.");
                return SplitTaskExecutionInfo.error(SplitTaskType.MY_SEGMENTS_REMOVAL);
            }
            HashSet<String> segments = new HashSet(new ArrayList(mMySegmentsStorage.getAll()));
            if(segments.remove(mSegmentName)) {
                mMySegmentsStorage.set(new ArrayList<>(segments));
                mEventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);
            }
        } catch (Exception e) {
            logError("Unknown error while removing segment " + mSegmentName + ": " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.MY_SEGMENTS_REMOVAL);
        }
        Logger.d("My Segments have been updated. Removed " + mSegmentName);
        return SplitTaskExecutionInfo.success(SplitTaskType.MY_SEGMENTS_REMOVAL);
    }

    private void logError(String message) {
        Logger.e("Error while executing my segments removal task: " + message);
    }
}
