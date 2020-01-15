package io.split.android.client.service.impressions;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImpressionsRecorderTask implements SplitTask {

    private final String mTaskId;
    private final SplitTaskExecutionListener mExecutionListener;
    private final PersistentImpressionsStorage mPersistenImpressionsStorage;
    private final HttpRecorder<List<KeyImpression>> mHttpRecorder;
    private final ImpressionsRecorderTaskConfig mConfig;

    public ImpressionsRecorderTask(@NonNull String taskId,
                                   @NonNull SplitTaskExecutionListener executionListener,
                                   @NonNull HttpRecorder<List<KeyImpression>> httpRecorder,
                                   @NonNull PersistentImpressionsStorage persistenEventsStorage,
                                   @NonNull ImpressionsRecorderTaskConfig config) {
        mTaskId = checkNotNull(taskId);
        mExecutionListener = checkNotNull(executionListener);
        mHttpRecorder = checkNotNull(httpRecorder);
        mPersistenImpressionsStorage = checkNotNull(persistenEventsStorage);
        mConfig = checkNotNull(config);
    }

    @Override
    public void execute() {
        SplitTaskExecutionStatus status = SplitTaskExecutionStatus.SUCCESS;
        List<KeyImpression> impressions;
        do {
            impressions = mPersistenImpressionsStorage.pop(mConfig.getImpressionsPerPush());
            if (impressions.size() > 0) {
                try {
                    mHttpRecorder.execute(impressions);
                } catch (HttpRecorderException e) {
                    status = SplitTaskExecutionStatus.ERROR;
                    Logger.e("Event recorder task: Some events couldn't be sent." +
                            "Saving to send them in a new iteration");
                    mPersistenImpressionsStorage.setActive(impressions);
                }
            }
        } while (impressions.size() == mConfig.getImpressionsPerPush());

        mExecutionListener.taskExecuted(
                new SplitTaskExecutionInfo(SplitTaskType.IMPRESSIONS_RECORDER, status,
                        0, 0));
    }
}
