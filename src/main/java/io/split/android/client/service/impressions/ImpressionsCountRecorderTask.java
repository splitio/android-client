package io.split.android.client.service.impressions;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.storage.impressions.PersistentImpressionsCountStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImpressionsCountRecorderTask implements SplitTask {
    private final PersistentImpressionsCountStorage mPersistentStorage;
    private final HttpRecorder<ImpressionsCount> mHttpRecorder;
    private static int POP_COUNT = ServiceConstants.DEFAULT_IMPRESSION_COUNT_ROWS_POP;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public ImpressionsCountRecorderTask(@NonNull HttpRecorder<ImpressionsCount> httpRecorder,
                                        @NonNull PersistentImpressionsCountStorage persistentStorage,
                                        @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        mHttpRecorder = checkNotNull(httpRecorder);
        mPersistentStorage = checkNotNull(persistentStorage);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        SplitTaskExecutionStatus status = SplitTaskExecutionStatus.SUCCESS;
        int nonSentRecords = 0;
        long nonSentBytes = 0;
        Integer httpErrorStatus = null;

        List<ImpressionsCountPerFeature> countList = new ArrayList<>();
        List<ImpressionsCountPerFeature> failedSent = new ArrayList<>();
        do {
            countList = mPersistentStorage.pop(POP_COUNT);
            if (countList.size() > 0) {
                long startTime = System.currentTimeMillis();
                try {
                    Logger.d("Posting %d Split impressions count", countList.size());
                    mHttpRecorder.execute(new ImpressionsCount(countList));
                    mPersistentStorage.delete(countList);
                    Logger.d("%d split impressions count sent", countList.size());
                } catch (HttpRecorderException e) {
                    status = SplitTaskExecutionStatus.ERROR;
                    Logger.e("Impressions count recorder task: Some counts couldn't be sent." +
                            "Saving to send them in a new iteration" +
                            e.getLocalizedMessage());
                    failedSent.addAll(countList);

                    httpErrorStatus = e.getHttpStatus();
                } finally {
                    mTelemetryRuntimeProducer.recordSyncLatency(OperationType.IMPRESSIONS_COUNT, System.currentTimeMillis() - startTime);
                }
            }
        } while (countList.size() == POP_COUNT);

        if(failedSent.size() > 0) {
            mPersistentStorage.setActive(failedSent);
        }

        if (status == SplitTaskExecutionStatus.ERROR) {
            Map<String, Object> data = (httpErrorStatus != null) ? Collections.singletonMap(SplitTaskExecutionInfo.HTTP_STATUS, httpErrorStatus) :
                    Collections.emptyMap();

            return SplitTaskExecutionInfo.error(
                    SplitTaskType.IMPRESSIONS_COUNT_RECORDER, data);
        }
        return SplitTaskExecutionInfo.success(SplitTaskType.IMPRESSIONS_COUNT_RECORDER);
    }
}
