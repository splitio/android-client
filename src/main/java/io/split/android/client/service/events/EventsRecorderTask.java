package io.split.android.client.service.events;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.service.HttpRequestBodySerializer;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventsRecorderTask implements SplitTask {

    private final String mTaskId;
    private final SplitTaskExecutionListener mExecutionListener;
    private final PersistentEventsStorage mPersistenEventsStorage;
    private final HttpRecorder<List<Event>> mHttpRecorder;
    private final EventsRecorderTaskConfig mConfig;

    public EventsRecorderTask(@NonNull String taskId,
                              @NonNull SplitTaskExecutionListener executionListener,
                              @NonNull HttpRecorder<List<Event>> httpRecorder,
                              @NonNull PersistentEventsStorage persistenEventsStorage,
                              @NonNull EventsRecorderTaskConfig config) {
        mTaskId = checkNotNull(taskId);
        mExecutionListener = checkNotNull(executionListener);
        mHttpRecorder = checkNotNull(httpRecorder);
        mPersistenEventsStorage = checkNotNull(persistenEventsStorage);
        mConfig = checkNotNull(config);
    }

    @Override
    public void execute() {
        SplitTaskExecutionStatus status = SplitTaskExecutionStatus.SUCCESS;
        boolean sendMore = true;
        while(sendMore) {
            List<Event> events = mPersistenEventsStorage.pop(mConfig.getEventsPerPush());
            if(events.size() > 0) {
                try {
                    mHttpRecorder.execute(events);
                } catch (HttpRecorderException e) {
                    status = SplitTaskExecutionStatus.ERROR;
                    Logger.e("Event recorder task: Some events couldn't be sent");
                }
            }
            sendMore = (events.size() == mConfig.getEventsPerPush());
        }

        mExecutionListener.taskExecuted(new SplitTaskExecutionInfo(mTaskId, status));
    }
}
