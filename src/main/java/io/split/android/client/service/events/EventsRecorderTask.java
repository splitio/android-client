package io.split.android.client.service.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventsRecorderTask implements SplitTask {

    private final SplitTaskType mTaskType;
    private final SplitTaskExecutionListener mExecutionListener;
    private final PersistentEventsStorage mPersistenEventsStorage;
    private final HttpRecorder<List<Event>> mHttpRecorder;
    private final EventsRecorderTaskConfig mConfig;

    public EventsRecorderTask(@NonNull SplitTaskType taskType,
                              @Nullable SplitTaskExecutionListener executionListener,
                              @NonNull HttpRecorder<List<Event>> httpRecorder,
                              @NonNull PersistentEventsStorage persistenEventsStorage,
                              @NonNull EventsRecorderTaskConfig config) {
        mTaskType = checkNotNull(taskType);
        mExecutionListener = checkNotNull(executionListener);
        mHttpRecorder = checkNotNull(httpRecorder);
        mPersistenEventsStorage = checkNotNull(persistenEventsStorage);
        mConfig = checkNotNull(config);
    }

    @Override
    public SplitTaskExecutionInfo execute() {
        SplitTaskExecutionStatus status = SplitTaskExecutionStatus.SUCCESS;
        int nonSentRecords = 0;
        long nonSentBytes = 0;
        boolean sendMore = true;
        List<Event> events;
        do {
            events = mPersistenEventsStorage.pop(mConfig.getEventsPerPush());
            if (events.size() > 0) {
                try {
                    Logger.d("Posting %d Split events", events.size());
                    mHttpRecorder.execute(events);
                } catch (HttpRecorderException e) {
                    status = SplitTaskExecutionStatus.ERROR;
                    nonSentRecords += mConfig.getEventsPerPush();
                    nonSentBytes += sumEventBytes(events);
                    Logger.e("Event recorder task: Some events couldn't be sent" +
                            "Saving to send them in a new iteration");
                    mPersistenEventsStorage.setActive(events);
                }
            }
        } while (events.size() == mConfig.getEventsPerPush());

        SplitTaskExecutionInfo info = new SplitTaskExecutionInfo(
                mTaskType, status, nonSentRecords, nonSentBytes);
        if(mExecutionListener != null) {
            mExecutionListener.taskExecuted(info);
        }
        return info;
    }

    private long sumEventBytes(List<Event> events) {
        long totalBytes = 0;
        for(Event event : events) {
            totalBytes += event.getSizeInBytes();
        }
        return totalBytes;
    }
}
