package io.split.android.client.impressions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import io.split.android.client.service.synchronizer.SyncManager;

public class SyncImpressionListenerTest {

    private SyncManager mSyncManager;
    private ExecutorService mExecutorService;

    @Before
    public void setUp() {
        mSyncManager = mock(SyncManager.class);
        mExecutorService = mock(ExecutorService.class);
    }

    @Test
    public void logSubmitsImpressionLoggingTaskInExecutor() {
        SyncImpressionListener syncImpressionListener = new SyncImpressionListener(mSyncManager, mExecutorService);
        DecoratedImpression impression = createImpression();

        syncImpressionListener.log(impression);

        verify(mExecutorService).submit(any(ImpressionLoggingTask.class));
    }

    @Test
    public void errorWhileSubmittingTaskIsHandled() {
        SyncImpressionListener syncImpressionListener = new SyncImpressionListener(mSyncManager, mExecutorService);
        DecoratedImpression impression = createImpression();
        doThrow(new RuntimeException("test")).when(mExecutorService).submit(any(ImpressionLoggingTask.class), any());

        syncImpressionListener.log(impression);
    }

    private static DecoratedImpression createImpression() {
        return new DecoratedImpression(new Impression("key", "feature", "treatment", "on", 1402040204L, "label", 123123L, new HashMap<>(), null), true);
    }
}
