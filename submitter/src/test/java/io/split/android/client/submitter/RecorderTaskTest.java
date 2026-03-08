package io.split.android.client.submitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;

@SuppressWarnings("unchecked")
public class RecorderTaskTest {

    private static final int BATCH_SIZE = 10;
    private static final SplitTaskType TASK_TYPE = SplitTaskType.IMPRESSIONS_RECORDER;

    private RecorderStorage<String> mStorage;
    private RecorderSubmitter<List<String>> mSubmitter;
    private RecorderTelemetry mTelemetry;

    @Before
    public void setUp() {
        mStorage = Mockito.mock(RecorderStorage.class);
        mSubmitter = Mockito.mock(RecorderSubmitter.class);
        mTelemetry = Mockito.mock(RecorderTelemetry.class);
    }

    // region Successful submission

    @Test
    public void successfulSingleBatchSubmission() throws RecorderException {
        List<String> batch = createItems(5); // less than BATCH_SIZE → loop terminates after one iteration
        when(mStorage.pop(BATCH_SIZE)).thenReturn(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        SplitTaskExecutionInfo result = task.execute();

        verify(mStorage, times(1)).pop(BATCH_SIZE);
        verify(mSubmitter, times(1)).execute(batch);
        verify(mStorage, times(1)).delete(batch);
        verify(mStorage, never()).setActive(any());
        assertEquals(TASK_TYPE, result.getTaskType());
        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
        assertNull(result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS));
        assertNull(result.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES));
        assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    @Test
    public void successfulMultiBatchSubmissionLoopsUntilSmallBatch() throws RecorderException {
        List<String> fullBatch = createItems(BATCH_SIZE);
        List<String> partialBatch = createItems(3);

        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(fullBatch)
                .thenReturn(fullBatch)
                .thenReturn(partialBatch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        SplitTaskExecutionInfo result = task.execute();

        // Three pops: full, full, partial (terminates)
        verify(mStorage, times(3)).pop(BATCH_SIZE);
        verify(mSubmitter, times(3)).execute(any());
        verify(mStorage, times(3)).delete(any());
        verify(mStorage, never()).setActive(any());
        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void emptyFirstPopSkipsSubmissionAndSucceeds() throws RecorderException {
        when(mStorage.pop(BATCH_SIZE)).thenReturn(new ArrayList<>());

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        SplitTaskExecutionInfo result = task.execute();

        verify(mStorage, times(1)).pop(BATCH_SIZE);
        verify(mSubmitter, never()).execute(any());
        verify(mStorage, never()).delete(any());
        verify(mStorage, never()).setActive(any());
        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    // endregion

    // region Error handling

    @Test
    public void retryableErrorCollectsFailuresAndContinuesLoop() throws RecorderException {
        List<String> batch = createItems(BATCH_SIZE);
        List<String> partialBatch = createItems(3);

        // First pop returns a full batch (fails), second also returns full (succeeds),
        // third returns partial (terminates)
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(batch)
                .thenReturn(partialBatch);

        // Throw only on the first call to execute; subsequent calls succeed
        doThrow(new RecorderException("retryable error", 500, true))
                .doNothing()
                .doNothing()
                .when(mSubmitter).execute(any());

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        SplitTaskExecutionInfo result = task.execute();

        // Three pops total: two full batches + one partial
        verify(mStorage, times(3)).pop(BATCH_SIZE);
        // First batch failed → not deleted; second and partial → deleted twice
        verify(mStorage, times(2)).delete(any());
        // Failing items (one batch worth) are re-queued
        verify(mStorage, times(1)).setActive(any());

        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        assertEquals(Integer.valueOf(BATCH_SIZE), result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS));
    }

    @Test
    public void retryableErrorPopulatesNonSentRecordsCount() throws RecorderException {
        List<String> batch = createItems(BATCH_SIZE);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        SplitTaskExecutionInfo result = task.execute();

        assertEquals(Integer.valueOf(BATCH_SIZE), result.getIntegerValue(SplitTaskExecutionInfo.NON_SENT_RECORDS));
        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void nonRetryableErrorStopsLoopImmediately() throws RecorderException {
        List<String> batch = createItems(BATCH_SIZE);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(batch); // Would be popped if loop continued
        doThrow(new RecorderException("non-retryable", 9009, false)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        SplitTaskExecutionInfo result = task.execute();

        // Only one pop — loop broke immediately on non-retryable error
        verify(mStorage, times(1)).pop(BATCH_SIZE);
        verify(mStorage, never()).delete(any());
        verify(mStorage, times(1)).setActive(any());

        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
        assertTrue(Boolean.TRUE.equals(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY)));
    }

    @Test
    public void retryableErrorDoesNotSetDoNotRetry() throws RecorderException {
        List<String> batch = createItems(BATCH_SIZE);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        SplitTaskExecutionInfo result = task.execute();

        assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
    }

    // endregion

    // region setActive

    @Test
    public void setActiveIsCalledWithFailedItemsOnError() throws RecorderException {
        List<String> batch = createItems(BATCH_SIZE);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        task.execute();

        verify(mStorage, times(1)).setActive(batch);
    }

    @Test
    public void setActiveIsNotCalledOnSuccess() throws RecorderException {
        List<String> batch = createItems(3);
        when(mStorage.pop(BATCH_SIZE)).thenReturn(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        task.execute();

        verify(mStorage, never()).setActive(any());
    }

    @Test
    public void chunkedSetActiveWhenFailingChunkSizeIsPositive() throws RecorderException {
        int failingChunkSize = 3;
        // Create items whose count is a multiple of failingChunkSize for predictable verification
        List<String> batch = createItems(9); // 9 items / chunkSize 3 = 3 setActive calls
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, failingChunkSize);
        task.execute();

        // 9 items chunked into 3 → 3 setActive calls
        verify(mStorage, times(3)).setActive(any());
    }

    @Test
    public void chunkedSetActiveHandlesNonEvenDivision() throws RecorderException {
        int failingChunkSize = 3;
        // 10 items / chunkSize 3 = 4 calls (chunks of 3, 3, 3, 1)
        List<String> batch = createItems(10);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, failingChunkSize);
        task.execute();

        verify(mStorage, times(4)).setActive(any());
    }

    @Test
    public void noChunkingWhenFailingChunkSizeIsZero() throws RecorderException {
        List<String> batch = createItems(BATCH_SIZE);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        task.execute();

        // No chunking → exactly one setActive call with all failing items
        verify(mStorage, times(1)).setActive(batch);
    }

    // endregion

    // region Byte tracking via estimateItemSize

    @Test
    public void byteTrackingViaEstimateItemSizeOverride() throws RecorderException {
        long itemSizeBytes = 50L;
        List<String> batch = createItems(BATCH_SIZE); // 10 items * 50 bytes = 500 bytes
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTaskWithItemSize(BATCH_SIZE, TASK_TYPE, mTelemetry, 0, itemSizeBytes);
        SplitTaskExecutionInfo result = task.execute();

        long expectedBytes = BATCH_SIZE * itemSizeBytes;
        assertEquals(Long.valueOf(expectedBytes), result.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES));
    }

    @Test
    public void byteTrackingDefaultsToZeroWhenNotOverridden() throws RecorderException {
        List<String> batch = createItems(BATCH_SIZE);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        SplitTaskExecutionInfo result = task.execute();

        // Default estimateItemSize returns 0 → nonSentBytes = 0
        assertEquals(Long.valueOf(0L), result.getLongValue(SplitTaskExecutionInfo.NON_SENT_BYTES));
    }

    // endregion

    // region transformForSubmission

    @Test
    public void transformForSubmissionHookIsApplied() throws RecorderException {
        List<String> batch = createItems(3);
        when(mStorage.pop(BATCH_SIZE)).thenReturn(batch);

        // Build a task with a custom transform that wraps items in a new list
        RecorderTask<String, List<String>> task = new SimpleRecorderTask(
                mStorage, mSubmitter, BATCH_SIZE, TASK_TYPE, mTelemetry, 0) {
            @Override
            protected List<String> transformForSubmission(List<String> items) {
                List<String> transformed = new ArrayList<>();
                for (String item : items) {
                    transformed.add(item.toUpperCase());
                }
                return transformed;
            }
        };
        task.execute();

        // The submitter should receive the transformed list (all uppercase)
        List<String> expectedTransformed = new ArrayList<>();
        for (String item : batch) {
            expectedTransformed.add(item.toUpperCase());
        }
        verify(mSubmitter, times(1)).execute(expectedTransformed);
    }

    // endregion

    // region Null telemetry

    @Test
    public void nullTelemetryDoesNotThrowNpeOnSuccess() throws RecorderException {
        List<String> batch = createItems(3);
        when(mStorage.pop(BATCH_SIZE)).thenReturn(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, null, 0);
        // Should not throw
        SplitTaskExecutionInfo result = task.execute();

        assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void nullTelemetryDoesNotThrowNpeOnError() throws RecorderException {
        List<String> batch = createItems(BATCH_SIZE);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, null, 0);
        // Should not throw
        SplitTaskExecutionInfo result = task.execute();

        assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    // endregion

    // region Telemetry interactions

    @Test
    public void telemetryRecordSuccessCalledOnSuccessfulSubmission() throws RecorderException {
        List<String> batch = createItems(3);
        when(mStorage.pop(BATCH_SIZE)).thenReturn(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        task.execute();

        verify(mTelemetry, times(1)).recordSuccess(anyLong());
    }

    @Test
    public void telemetryRecordSuccessCalledOncePerBatch() throws RecorderException {
        List<String> fullBatch = createItems(BATCH_SIZE);
        List<String> partialBatch = createItems(3);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(fullBatch)
                .thenReturn(partialBatch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        task.execute();

        verify(mTelemetry, times(2)).recordSuccess(anyLong());
    }

    @Test
    public void telemetryRecordLatencyCalledOnSuccess() throws RecorderException {
        List<String> batch = createItems(3);
        when(mStorage.pop(BATCH_SIZE)).thenReturn(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        task.execute();

        verify(mTelemetry, atLeastOnce()).recordLatency(anyLong());
    }

    @Test
    public void telemetryRecordLatencyCalledOnError() throws RecorderException {
        List<String> batch = createItems(BATCH_SIZE);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        task.execute();

        verify(mTelemetry, atLeastOnce()).recordLatency(anyLong());
    }

    @Test
    public void telemetryRecordErrorCalledWithHttpStatusOnError() throws RecorderException {
        int httpStatus = 500;
        List<String> batch = createItems(BATCH_SIZE);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", httpStatus, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        task.execute();

        verify(mTelemetry, times(1)).recordError(httpStatus);
    }

    @Test
    public void telemetryRecordSuccessNotCalledOnError() throws RecorderException {
        List<String> batch = createItems(BATCH_SIZE);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        task.execute();

        verify(mTelemetry, never()).recordSuccess(anyLong());
    }

    @Test
    public void telemetryRecordErrorNotCalledOnSuccess() throws RecorderException {
        List<String> batch = createItems(3);
        when(mStorage.pop(BATCH_SIZE)).thenReturn(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        task.execute();

        verify(mTelemetry, never()).recordError(any(Integer.class));
    }

    // endregion

    // region Task type

    @Test
    public void taskTypeIsPreservedInSuccessResult() throws RecorderException {
        List<String> batch = createItems(3);
        when(mStorage.pop(BATCH_SIZE)).thenReturn(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        SplitTaskExecutionInfo result = task.execute();

        assertEquals(TASK_TYPE, result.getTaskType());
    }

    @Test
    public void taskTypeIsPreservedInErrorResult() throws RecorderException {
        List<String> batch = createItems(BATCH_SIZE);
        when(mStorage.pop(BATCH_SIZE))
                .thenReturn(batch)
                .thenReturn(new ArrayList<>());
        doThrow(new RecorderException("error", 500, true)).when(mSubmitter).execute(batch);

        RecorderTask<String, List<String>> task = buildTask(BATCH_SIZE, TASK_TYPE, mTelemetry, 0);
        SplitTaskExecutionInfo result = task.execute();

        assertEquals(TASK_TYPE, result.getTaskType());
    }

    // endregion

    // region Helpers

    private List<String> createItems(int count) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add("item_" + i);
        }
        return items;
    }

    /**
     * Builds a standard {@link RecorderTask} with no custom overrides.
     * Uses the default {@link RecorderTask#transformForSubmission} (identity cast) and
     * {@link RecorderTask#estimateItemSize} (returns 0).
     */
    private RecorderTask<String, List<String>> buildTask(int batchSize,
                                                          SplitTaskType taskType,
                                                          RecorderTelemetry telemetry,
                                                          int failingChunkSize) {
        return new SimpleRecorderTask(mStorage, mSubmitter, batchSize, taskType, telemetry, failingChunkSize);
    }

    /**
     * Builds a {@link RecorderTask} with a custom fixed item size returned from
     * {@link RecorderTask#estimateItemSize}, to exercise byte tracking.
     */
    private RecorderTask<String, List<String>> buildTaskWithItemSize(int batchSize,
                                                                      SplitTaskType taskType,
                                                                      RecorderTelemetry telemetry,
                                                                      int failingChunkSize,
                                                                      long itemSizeBytes) {
        return new SimpleRecorderTask(mStorage, mSubmitter, batchSize, taskType, telemetry, failingChunkSize) {
            @Override
            protected long estimateItemSize(String item) {
                return itemSizeBytes;
            }
        };
    }

    /**
     * Minimal concrete subclass of {@link RecorderTask} for testing.
     * T = String, R = List&lt;String&gt; (identity transform via default unchecked cast).
     */
    private static class SimpleRecorderTask extends RecorderTask<String, List<String>> {

        SimpleRecorderTask(@NonNull RecorderStorage<String> storage,
                           @NonNull RecorderSubmitter<List<String>> submitter,
                           int batchSize,
                           @NonNull SplitTaskType taskType,
                           RecorderTelemetry telemetry,
                           int failingChunkSize) {
            super(storage, submitter, batchSize, taskType, telemetry, failingChunkSize);
        }
    }

    // endregion
}
