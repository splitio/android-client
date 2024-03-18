package io.split.android.client.impressions;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.synchronizer.SyncManager;

public class ImpressionLoggingTaskTest {

    private SyncManager mSyncManager;
    private ImpressionLoggingTask mImpressionsLoggingTask;

    @Before
    public void setUp() {
        mSyncManager = mock(SyncManager.class);
    }

    @Test
    public void executeLogsImpressionInListener() {
        Impression impression = createImpression();
        mImpressionsLoggingTask = new ImpressionLoggingTask(mSyncManager, impression);

        mImpressionsLoggingTask.execute();

        verify(mSyncManager).pushImpression(impression);
    }

    @Test
    public void successfulExecutionReturnsSuccessInfo() {
        Impression impression = createImpression();
        mImpressionsLoggingTask = new ImpressionLoggingTask(mSyncManager, impression);

        SplitTaskExecutionInfo result = mImpressionsLoggingTask.execute();

        assertEquals(result.getStatus(), SplitTaskExecutionStatus.SUCCESS);
        assertEquals(result.getTaskType(), SplitTaskType.GENERIC_TASK);
    }

    @Test
    public void unsuccessfulExecutionReturnsFailureInfo() {
        doThrow(new RuntimeException("test")).when(mSyncManager).pushImpression(any(Impression.class));
        Impression impression = createImpression();
        mImpressionsLoggingTask = new ImpressionLoggingTask(mSyncManager, impression);

        SplitTaskExecutionInfo result = mImpressionsLoggingTask.execute();

        assertEquals(result.getStatus(), SplitTaskExecutionStatus.ERROR);
        assertEquals(result.getTaskType(), SplitTaskType.GENERIC_TASK);
    }

    private static Impression createImpression() {
        return new Impression("key", "feature", "treatment", "on", 1402040204L, "label", 123123L, new HashMap<>());
    }
}
