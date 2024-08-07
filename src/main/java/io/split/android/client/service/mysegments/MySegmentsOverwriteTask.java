package io.split.android.client.service.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.synchronizer.MySegmentsChangeChecker;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.utils.logger.Logger;

public class MySegmentsOverwriteTask implements SplitTask {

    private final List<String> mMySegments;
    private final long mChangeNumber;
    private final MySegmentsStorage mMySegmentsStorage;
    private final SplitEventsManager mEventsManager;
    private MySegmentsChangeChecker mMySegmentsChangeChecker;
    private final SplitTaskType mTaskType;
    private final SplitInternalEvent mUpdateEvent;

    public MySegmentsOverwriteTask(@NonNull MySegmentsStorage mySegmentsStorage,
                                   List<String> mySegments,
                                   Long changeNumber,
                                   SplitEventsManager eventsManager,
                                   MySegmentsOverwriteTaskConfig config) {
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
        mMySegments = mySegments;
        mChangeNumber = changeNumber == null ? -1 : changeNumber;
        mEventsManager = eventsManager;
        mMySegmentsChangeChecker = new MySegmentsChangeChecker();
        mTaskType = config.getTaskType();
        mUpdateEvent = config.getInternalEvent();
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        try {
            if (mMySegments == null) {
                logError("My segment list could not be null.");
                return SplitTaskExecutionInfo.error(mTaskType);
            }
            List<String> oldSegments = new ArrayList<>(mMySegmentsStorage.getAll());
            if (mMySegmentsChangeChecker.mySegmentsHaveChanged(oldSegments, mMySegments)) {
                mMySegmentsStorage.set(mMySegments, mChangeNumber);
                mEventsManager.notifyInternalEvent(mUpdateEvent);
            }
        } catch (Exception e) {
            logError("Unknown error while overwriting my segments: " + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(mTaskType);
        }
        Logger.d("My Segments have been overwritten");
        return SplitTaskExecutionInfo.success(mTaskType);
    }

    private void logError(String message) {
        Logger.e("Error while executing my segments overwrite task: " + message);
    }

    @VisibleForTesting
    public void setChangesChecker(MySegmentsChangeChecker changesChecker) {
        mMySegmentsChangeChecker = changesChecker;
    }
}
