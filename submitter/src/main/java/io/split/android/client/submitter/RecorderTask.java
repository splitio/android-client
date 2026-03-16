package io.split.android.client.submitter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.utils.logger.Logger;

/**
 * Abstract base class for batch submission tasks.
 * <p>
 * Encapsulates the common pop-submit-retry-setActive pattern used by
 * impressions, events, and other batch recorder tasks.
 *
 * @param <T> The storage item type (e.g., KeyImpression, Event)
 * @param <R> The submission payload type (e.g., List<KeyImpression>, ImpressionsCount)
 */
public abstract class RecorderTask<T, R> implements SplitTask {

    private final RecorderStorage<T> mStorage;
    private final RecorderSubmitter<R> mSubmitter;
    private final int mBatchSize;
    private final SplitTaskType mTaskType;
    @Nullable
    private final RecorderTelemetry mTelemetry;
    private final int mFailingChunkSize; // 0 = no chunking

    protected RecorderTask(@NonNull RecorderStorage<T> storage,
                           @NonNull RecorderSubmitter<R> submitter,
                           int batchSize,
                           @NonNull SplitTaskType taskType,
                           @Nullable RecorderTelemetry telemetry,
                           int failingChunkSize) {
        mStorage = storage;
        mSubmitter = submitter;
        mBatchSize = batchSize;
        mTaskType = taskType;
        mTelemetry = telemetry;
        mFailingChunkSize = failingChunkSize;
    }

    @NonNull
    @Override
    public final SplitTaskExecutionInfo execute() {
        SplitTaskExecutionStatus status = SplitTaskExecutionStatus.SUCCESS;
        int nonSentRecords = 0;
        long nonSentBytes = 0;
        List<T> items;
        List<T> failingItems = new ArrayList<>();
        boolean doNotRetry = false;

        do {
            items = mStorage.pop(mBatchSize);
            if (!items.isEmpty()) {
                long startTime = System.currentTimeMillis();
                try {
                    R payload = transformForSubmission(items);
                    mSubmitter.execute(payload);

                    long now = System.currentTimeMillis();
                    if (mTelemetry != null) {
                        mTelemetry.recordSuccess(now);
                    }

                    mStorage.delete(items);
                } catch (RecorderException e) {
                    status = SplitTaskExecutionStatus.ERROR;
                    nonSentRecords += items.size();
                    nonSentBytes += sumBytes(items);
                    Logger.e("RecorderTask: " + items.size() + " items couldn't be submitted. " +
                            "Saving to retry in a new iteration: " + e.getLocalizedMessage());
                    failingItems.addAll(items);

                    if (mTelemetry != null) {
                        mTelemetry.recordError(e.getHttpStatus());
                    }

                    if (!e.isRetryable()) {
                        doNotRetry = true;
                        break;
                    }
                } finally {
                    if (mTelemetry != null) {
                        mTelemetry.recordLatency(System.currentTimeMillis() - startTime);
                    }
                }
            }
        } while (items.size() == mBatchSize);

        // Re-queue failed items for retry
        if (!failingItems.isEmpty()) {
            if (mFailingChunkSize > 0) {
                // Chunk to avoid SQLite errors (used by EventsRecorderTask)
                int size = failingItems.size();
                for (int i = 0; i < size; i += mFailingChunkSize) {
                    mStorage.setActive(failingItems.subList(i, Math.min(i + mFailingChunkSize, size)));
                }
            } else {
                mStorage.setActive(failingItems);
            }
        }

        if (status == SplitTaskExecutionStatus.ERROR) {
            Map<String, Object> data = new HashMap<>();
            data.put(SplitTaskExecutionInfo.NON_SENT_RECORDS, nonSentRecords);
            data.put(SplitTaskExecutionInfo.NON_SENT_BYTES, nonSentBytes);
            if (doNotRetry) {
                data.put(SplitTaskExecutionInfo.DO_NOT_RETRY, true);
            }
            return SplitTaskExecutionInfo.error(mTaskType, data);
        }

        return SplitTaskExecutionInfo.success(mTaskType);
    }

    /**
     * Transform storage items into the submission payload before submitting.
     */
    protected abstract R transformForSubmission(List<T> items);

    /**
     * Estimate the byte size of one storage item for tracking non-sent bytes.
     * <p>
     * Default returns 0. Override to enable byte tracking.
     */
    protected long estimateItemSize(T item) {
        return 0;
    }

    private long sumBytes(List<T> items) {
        long total = 0;
        for (T item : items) {
            total += estimateItemSize(item);
        }
        return total;
    }
}
