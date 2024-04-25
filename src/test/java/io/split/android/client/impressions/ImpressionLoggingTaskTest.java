package io.split.android.client.impressions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

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

        mImpressionsLoggingTask.run();

        verify(mSyncManager).pushImpression(impression);
    }

    @Test
    public void successfulExecutionReturnsSuccessInfo() {
        Impression impression = createImpression();
        mImpressionsLoggingTask = new ImpressionLoggingTask(mSyncManager, impression);

        mImpressionsLoggingTask.run();
    }

    @Test
    public void unsuccessfulExecutionDoesNotCrash() {
        doThrow(new RuntimeException("test")).when(mSyncManager).pushImpression(any(Impression.class));
        Impression impression = createImpression();
        mImpressionsLoggingTask = new ImpressionLoggingTask(mSyncManager, impression);

        mImpressionsLoggingTask.run();
    }

    private static Impression createImpression() {
        return new Impression("key", "feature", "treatment", "on", 1402040204L, "label", 123123L, new HashMap<>());
    }
}
