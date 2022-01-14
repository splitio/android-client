package io.split.android.client.service.impressions;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public final static int FAILING_CHUNK_SIZE = 20;
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
        List<KeyImpression> failingImpressions = new ArrayList<>();
        Integer httpErrorStatus = null;
        do {
            impressions = mPersistenImpressionsStorage.pop(mConfig.getImpressionsPerPush());
            if (impressions.size() > 0) {
                try {
                    Logger.d("Posting %d Split impressions", impressions.size());
                    mHttpRecorder.execute(impressions);
                    mPersistenImpressionsStorage.delete(impressions);
                    Logger.d("%d split impressions sent", impressions.size());
                } catch (HttpRecorderException e) {
                    status = SplitTaskExecutionStatus.ERROR;
                    nonSentRecords += mConfig.getImpressionsPerPush();
                    nonSentBytes += sumImpressionsBytes(impressions);
                    Logger.e("Impressions recorder task: Some impressions couldn't be sent." +
                            "Saving to send them in a new iteration" +
                            e.getLocalizedMessage());
                    failingImpressions.addAll(impressions);
                    httpErrorStatus = e.getHttpStatus();
                }
            }
        } while (impressions.size() == mConfig.getImpressionsPerPush());

        if (failingImpressions.size() > 0) {
            mPersistenImpressionsStorage.setActive(failingImpressions);
        }

        if (status == SplitTaskExecutionStatus.ERROR) {
            Map<String, Object> data = new HashMap<>();
            data.put(SplitTaskExecutionInfo.NON_SENT_RECORDS, nonSentRecords);
            data.put(SplitTaskExecutionInfo.NON_SENT_BYTES, nonSentBytes);
            if (httpErrorStatus != null) {
                data.put(SplitTaskExecutionInfo.HTTP_STATUS, httpErrorStatus);
            }
            return SplitTaskExecutionInfo.error(
                    SplitTaskType.IMPRESSIONS_RECORDER, data);
        }
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
