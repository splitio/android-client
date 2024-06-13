package io.split.android.client.service.impressions;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
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
import io.split.android.client.service.http.HttpStatus;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.client.utils.logger.Logger;

public class ImpressionsRecorderTask implements SplitTask {
    public final static int FAILING_CHUNK_SIZE = 20;
    private final PersistentImpressionsStorage mPersistenImpressionsStorage;
    private final HttpRecorder<List<KeyImpression>> mHttpRecorder;
    private final ImpressionsRecorderTaskConfig mConfig;
    private final TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    public ImpressionsRecorderTask(@NonNull HttpRecorder<List<KeyImpression>> httpRecorder,
                                   @NonNull PersistentImpressionsStorage persistenEventsStorage,
                                   @NonNull ImpressionsRecorderTaskConfig config,
                                   @NonNull TelemetryRuntimeProducer telemetryRuntimeProducer) {
        mHttpRecorder = checkNotNull(httpRecorder);
        mPersistenImpressionsStorage = checkNotNull(persistenEventsStorage);
        mConfig = checkNotNull(config);
        mTelemetryRuntimeProducer = checkNotNull(telemetryRuntimeProducer);
    }

    @Override
    @NonNull
    public SplitTaskExecutionInfo execute() {
        SplitTaskExecutionStatus status = SplitTaskExecutionStatus.SUCCESS;
        int nonSentRecords = 0;
        long nonSentBytes = 0;
        List<KeyImpression> impressions;
        List<KeyImpression> failingImpressions = new ArrayList<>();
        boolean doNotRetry = false;
        do {
            impressions = mPersistenImpressionsStorage.pop(mConfig.getImpressionsPerPush());
            if (impressions.size() > 0) {
                long startTime = System.currentTimeMillis();
                long latency = 0;
                try {
                    Logger.d("Posting %d Split impressions", impressions.size());
                    mHttpRecorder.execute(impressions);

                    long now = System.currentTimeMillis();
                    latency = now - startTime;
                    mTelemetryRuntimeProducer.recordSuccessfulSync(OperationType.IMPRESSIONS, now);

                    mPersistenImpressionsStorage.delete(impressions);
                    Logger.d("%d split impressions sent", impressions.size());
                } catch (HttpRecorderException e) {
                    status = SplitTaskExecutionStatus.ERROR;
                    nonSentRecords += mConfig.getImpressionsPerPush();
                    nonSentBytes += sumImpressionsBytes(impressions);
                    Logger.e("Impressions recorder task: Some impressions couldn't be sent. " +
                            "Saving to send them in a new iteration\n" +
                            e.getLocalizedMessage());
                    failingImpressions.addAll(impressions);

                    mTelemetryRuntimeProducer.recordSyncError(OperationType.IMPRESSIONS, e.getHttpStatus());

                    if (HttpStatus.isNotRetryable(HttpStatus.fromCode(e.getHttpStatus()))) {
                        doNotRetry = true;
                        break;
                    }
                } finally {
                    mTelemetryRuntimeProducer.recordSyncLatency(OperationType.IMPRESSIONS, latency);
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
            if (doNotRetry) {
                data.put(SplitTaskExecutionInfo.DO_NOT_RETRY, true);
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
