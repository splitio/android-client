package io.split.android.client.service.impressions;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImpressionsRecorderTask implements SplitTask {

    private final PersistentImpressionsStorage mPersistenImpressionsStorage;
    private final HttpRecorder<List<KeyImpression>> mHttpRecorder;
    private final ImpressionsRecorderTaskConfig mConfig;

    public ImpressionsRecorderTask(@NonNull HttpRecorder<List<KeyImpression>> httpRecorder,
                                   @NonNull PersistentImpressionsStorage persistenEventsStorage,
                                   @NonNull ImpressionsRecorderTaskConfig config) {
        mHttpRecorder = checkNotNull(httpRecorder);
        mPersistenImpressionsStorage = checkNotNull(persistenEventsStorage);
        mConfig = checkNotNull(config);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        SplitTaskExecutionStatus status = SplitTaskExecutionStatus.SUCCESS;
        int nonSentRecords = 0;
        long nonSentBytes = 0;
        List<KeyImpression> impressions;
        do {
            impressions = mPersistenImpressionsStorage.pop(mConfig.getImpressionsPerPush());
            if (impressions.size() > 0) {
                try {
                    Logger.d("Posting %d Split impressions", impressions.size());
                    mHttpRecorder.execute(impressions);
                    Logger.d("%d split impressions sent", impressions.size());
                } catch (HttpRecorderException e) {
                    status = SplitTaskExecutionStatus.ERROR;
                    nonSentRecords += mConfig.getImpressionsPerPush();
                    nonSentBytes += sumImpressionsBytes(impressions);
                    Logger.e("Impressions recorder task: Some impressions couldn't be sent." +
                            "Saving to send them in a new iteration" +
                            e.getLocalizedMessage());
                    mPersistenImpressionsStorage.setActive(impressions);
                }
            }
        } while (impressions.size() == mConfig.getImpressionsPerPush());

        if (status == SplitTaskExecutionStatus.ERROR) {
            return SplitTaskExecutionInfo.error(
                    SplitTaskType.IMPRESSIONS_RECORDER,
                    nonSentRecords, nonSentBytes);
        }
        Logger.d("Posting %d Split impressions", impressions.size());
        return SplitTaskExecutionInfo.success(SplitTaskType.IMPRESSIONS_RECORDER);
    }

    private long sumImpressionsBytes(List<KeyImpression> impressions) {
        long totalBytes = 0;
        for (KeyImpression impression : impressions) {
            totalBytes += mConfig.getEstimatedSizeInBytes();
        }
        return totalBytes;
    }
}
