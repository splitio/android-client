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
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImpressionsRecorderTask implements SplitTask {

    private final SplitTaskType mTaskType;
    private final SplitTaskExecutionListener mExecutionListener;
    private final PersistentImpressionsStorage mPersistenImpressionsStorage;
    private final HttpRecorder<List<KeyImpression>> mHttpRecorder;
    private final ImpressionsRecorderTaskConfig mConfig;

    public ImpressionsRecorderTask(@NonNull SplitTaskType taskType,
                                   @NonNull SplitTaskExecutionListener executionListener,
                                   @NonNull HttpRecorder<List<KeyImpression>> httpRecorder,
                                   @NonNull PersistentImpressionsStorage persistenEventsStorage,
                                   @NonNull ImpressionsRecorderTaskConfig config) {
        mTaskType = checkNotNull(taskType);
        mExecutionListener = checkNotNull(executionListener);
        mHttpRecorder = checkNotNull(httpRecorder);
        mPersistenImpressionsStorage = checkNotNull(persistenEventsStorage);
        mConfig = checkNotNull(config);
    }

    @Override
    public void execute() {
        long initialTime = System.currentTimeMillis();
        SplitTaskExecutionStatus status = SplitTaskExecutionStatus.SUCCESS;
        int nonSentRecords = 0;
        long nonSentBytes = 0;
        boolean sendMore = true;
        List<KeyImpression> impressions;
        do {
            impressions = mPersistenImpressionsStorage.pop(mConfig.getImpressionsPerPush());
            if(impressions.size() > 0) {
                try {
                    Logger.d("Posting %d Split impressions", impressions.size());
                    mHttpRecorder.execute(impressions);
                } catch (HttpRecorderException e) {
                    status = SplitTaskExecutionStatus.ERROR;
                    nonSentRecords += mConfig.getImpressionsPerPush();
                    nonSentBytes += sumImpressionsBytes(impressions);
                    Logger.e("Event recorder task: Some events couldn't be sent." +
                            "Saving to send them in a new iteration");
                    mPersistenImpressionsStorage.setActive(impressions);
                }
            }
        } while (impressions.size() == mConfig.getImpressionsPerPush());

        Logger.d("Posting Split impressions took %d millis", (System.currentTimeMillis() - initialTime));
        mExecutionListener.taskExecuted(
                new SplitTaskExecutionInfo(SplitTaskType.IMPRESSIONS_RECORDER, status,
                        nonSentRecords, nonSentBytes));
    }

    private long sumImpressionsBytes(List<KeyImpression> impressions) {
        long totalBytes = 0;
        for(KeyImpression impression : impressions) {
            totalBytes += mConfig.getEstimatedSizeInBytes();
        }
        return totalBytes;
    }
}
