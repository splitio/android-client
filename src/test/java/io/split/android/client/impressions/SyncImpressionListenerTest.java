package io.split.android.client.impressions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.synchronizer.SyncManager;

public class SyncImpressionListenerTest {

    private SyncManager mSyncManager;
    private SplitTaskExecutor mSplitTaskExecutor;

    @Before
    public void setUp() {
        mSyncManager = mock(SyncManager.class);
        mSplitTaskExecutor = mock(SplitTaskExecutor.class);
    }

    @Test
    public void logSubmitsImpressionLoggingTaskInExecutor() {
        SyncImpressionListener syncImpressionListener = new SyncImpressionListener(mSyncManager, mSplitTaskExecutor);
        Impression impression = createImpression();

        syncImpressionListener.log(impression);

        verify(mSplitTaskExecutor).submit(any(ImpressionLoggingTask.class), any());
    }

    @Test
    public void errorWhileSubmittingTaskIsHandled() {
        SyncImpressionListener syncImpressionListener = new SyncImpressionListener(mSyncManager, mSplitTaskExecutor);
        Impression impression = createImpression();
        doThrow(new RuntimeException("test")).when(mSplitTaskExecutor).submit(any(ImpressionLoggingTask.class), any());

        syncImpressionListener.log(impression);
    }

    private static Impression createImpression() {
        return new Impression("key", "feature", "treatment", "on", 1402040204L, "label", 123123L, new HashMap<>());
    }
}
