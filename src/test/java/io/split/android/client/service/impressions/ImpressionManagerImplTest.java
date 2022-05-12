package io.split.android.client.service.impressions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.impressions.unique.SaveUniqueImpressionsTask;
import io.split.android.client.service.impressions.unique.UniqueKeysRecorderTask;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.fake.SplitTaskExecutorStub;

public class ImpressionManagerImplTest {

    private ImpressionManagerImpl mImpressionsManager;

    private SplitTaskExecutor mTaskExecutor;

    @Mock
    private ImpressionsTaskFactory mTaskFactory;

    @Mock
    private TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    @Mock
    private PersistentImpressionsStorage mImpressionsStorage;

    @Mock
    private UniqueKeysTracker mUniqueKeysTracker;

    private ImpressionManagerImpl.ImpressionManagerConfig mConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mTaskExecutor = spy(new SplitTaskExecutorStub());

        when(mTaskFactory.createImpressionsRecorderTask()).thenReturn(mock(ImpressionsRecorderTask.class));
        when(mTaskFactory.createImpressionsCountRecorderTask()).thenReturn(mock(ImpressionsCountRecorderTask.class));
        when(mTaskFactory.createSaveImpressionsCountTask(any())).thenReturn(mock(SaveImpressionsCountTask.class));
        when(mTaskFactory.createSaveUniqueImpressionsTask(any())).thenReturn(mock(SaveUniqueImpressionsTask.class));
        when(mTaskFactory.createUniqueImpressionsRecorderTask()).thenReturn(mock(UniqueKeysRecorderTask.class));

        mConfig = new ImpressionManagerImpl.ImpressionManagerConfig(
                1800,
                1800,
                ImpressionsMode.OPTIMIZED,
                3,
                2048,
                500
        );
        mImpressionsManager = new ImpressionManagerImpl(mTaskExecutor, mTaskFactory, mTelemetryRuntimeProducer,
                mImpressionsStorage, mUniqueKeysTracker, mConfig);
    }

    @Test
    public void pushImpression() throws InterruptedException {
        Impression impression = createImpression();
        ArgumentCaptor<KeyImpression> impressionCaptor = ArgumentCaptor.forClass(KeyImpression.class);
        mImpressionsManager.startPeriodicRecording();
        mImpressionsManager.pushImpression(impression);
        Thread.sleep(500);
        verify(mTaskExecutor, times(0)).submit(
                any(ImpressionsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
        verify(mImpressionsStorage, times(1)).push(impressionCaptor.capture());
        assertEquals("key", impressionCaptor.getValue().keyName);
        assertEquals("bkey", impressionCaptor.getValue().bucketingKey);
        assertEquals("split", impressionCaptor.getValue().feature);
        assertEquals("on", impressionCaptor.getValue().treatment);
        assertEquals(100L, impressionCaptor.getValue().time);
        assertEquals("default rule", impressionCaptor.getValue().label);
        assertEquals(999, impressionCaptor.getValue().changeNumber.longValue());
    }

    @Test
    public void pushImpressionReachQueueSizeImpDebug() throws InterruptedException {
        mImpressionsManager = getDebugModeManager();
        mImpressionsManager.startPeriodicRecording();
        for (int i = 0; i < 8; i++) {
            mImpressionsManager.pushImpression(createImpression());
        }
        Thread.sleep(200);
        verify(mImpressionsStorage, times(8)).push(any(KeyImpression.class));
        verify(mTaskExecutor, times(2)).submit(
                any(ImpressionsRecorderTask.class),
                any(RecorderSyncHelper.class));
    }

    @Test
    public void pushImpressionReachQueueSizeImpOptimized() throws InterruptedException {
        for (int i = 0; i < 8; i++) {
            mImpressionsManager.pushImpression(createUniqueImpression());
        }
        Thread.sleep(200);
        verify(mImpressionsStorage, times(8)).push(any(KeyImpression.class));
        verify(mTaskExecutor, times(2)).submit(
                any(ImpressionsRecorderTask.class),
                any(RecorderSyncHelper.class));
    }

    @Test
    public void pushImpressionBytesLimitImpDebug() throws InterruptedException {
        mImpressionsManager = getDebugModeManager();
        for (int i = 0; i < 10; i++) {
            mImpressionsManager.pushImpression(createImpression());
        }
        Thread.sleep(200);
        verify(mImpressionsStorage, times(10)).push(any(KeyImpression.class));
        verify(mTaskExecutor, times(2)).submit(
                any(ImpressionsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
    }

    @Test
    public void pushImpressionBytesLimitImpOptimized() throws InterruptedException {

        for (int i = 0; i < 10; i++) {
            mImpressionsManager.pushImpression(createUniqueImpression());
        }
        Thread.sleep(200);
        verify(mImpressionsStorage, times(10)).push(any(KeyImpression.class));
        verify(mTaskExecutor, times(2)).submit(
                any(ImpressionsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
    }

    @Test
    public void flushWithOptimizedMode() {

        ArgumentCaptor<List<SplitTaskBatchItem>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);

        mImpressionsManager.flush();

        verify(mTaskExecutor).submit(argThat(argument -> argument instanceof ImpressionsRecorderTask), any(RecorderSyncHelper.class));
        verify(mTaskExecutor).executeSerially(listArgumentCaptor.capture());

        assertTrue(listArgumentCaptor.getValue().get(0).getTask() instanceof SaveImpressionsCountTask);
        assertTrue(listArgumentCaptor.getValue().get(1).getTask() instanceof ImpressionsCountRecorderTask);
    }

    @Test
    public void flushWithDebugMode() {
        mImpressionsManager = getDebugModeManager();

        mImpressionsManager.flush();

        verify(mTaskExecutor).submit(argThat(argument -> argument instanceof ImpressionsRecorderTask), any(RecorderSyncHelper.class));
        verify(mTaskExecutor, times(0)).executeSerially(any());
    }

    @Test
    public void flushWithNoneMode() {
        mImpressionsManager = getNoneModeManager();

        mImpressionsManager.flush();

        verify(mTaskExecutor).executeSerially(argThat(new ArgumentMatcher<List<SplitTaskBatchItem>>() {
            @Override
            public boolean matches(List<SplitTaskBatchItem> argument) {
                return argument.size() == 2 && argument.get(0).getTask() instanceof SaveImpressionsCountTask && argument.get(1).getTask() instanceof ImpressionsCountRecorderTask;
            }
        }));

        verify(mTaskExecutor).executeSerially(argThat(new ArgumentMatcher<List<SplitTaskBatchItem>>() {
            @Override
            public boolean matches(List<SplitTaskBatchItem> argument) {
                return argument.size() == 2 && argument.get(0).getTask() instanceof SaveUniqueImpressionsTask && argument.get(1).getTask() instanceof UniqueKeysRecorderTask;
            }
        }));
    }

    @Test
    public void startPeriodicRecording() {
        mImpressionsManager.startPeriodicRecording();

        verify(mTaskExecutor).schedule(any(ImpressionsRecorderTask.class), eq(0L), eq(1800L), any(RecorderSyncHelper.class));
        verify(mTaskExecutor).schedule(any(ImpressionsCountRecorderTask.class), eq(0L), eq(1800L), eq(null));
    }

    @Test
    public void startPeriodicRecordingDebugMode() {
        mImpressionsManager = getDebugModeManager();

        mImpressionsManager.startPeriodicRecording();

        verify(mTaskExecutor).schedule(any(ImpressionsRecorderTask.class), eq(0L), eq(1800L), any(RecorderSyncHelper.class));
        verify(mTaskExecutor, times(0)).schedule(any(ImpressionsCountRecorderTask.class), eq(0L), eq(1800L), eq(null));
    }

    @Test
    public void startPeriodicRecordingNoneMode() {
        mImpressionsManager = getNoneModeManager();

        mImpressionsManager.startPeriodicRecording();

        verify(mTaskExecutor).schedule(any(UniqueKeysRecorderTask.class), eq(0L), eq(500L), eq(null));
        verify(mTaskExecutor).schedule(any(ImpressionsCountRecorderTask.class), eq(0L), eq(1800L), eq(null));
    }

    @Test
    public void stopPeriodicRecording() {
        when(mTaskExecutor.schedule(any(ImpressionsRecorderTask.class), eq(0L), eq(1800L), any(RecorderSyncHelper.class))).thenReturn("id_1");
        when(mTaskExecutor.schedule(any(ImpressionsCountRecorderTask.class), eq(0L), eq(1800L), eq(null))).thenReturn("id_2");

        mImpressionsManager.startPeriodicRecording();
        mImpressionsManager.stopPeriodicRecording();

        verify(mTaskExecutor).submit(any(SaveImpressionsCountTask.class), eq(null));
        verify(mTaskExecutor).stopTask("id_1");
        verify(mTaskExecutor).stopTask("id_2");
    }

    @Test
    public void stopPeriodicRecordingDebugMode() {
        mImpressionsManager = getDebugModeManager();

        when(mTaskExecutor.schedule(any(ImpressionsRecorderTask.class), eq(0L), eq(1800L), any(RecorderSyncHelper.class))).thenReturn("id_1");

        mImpressionsManager.startPeriodicRecording();
        mImpressionsManager.stopPeriodicRecording();

        verify(mTaskExecutor, times(0)).submit(any(SaveImpressionsCountTask.class), eq(null));
        verify(mTaskExecutor).stopTask("id_1");
        verify(mTaskExecutor, times(2)).stopTask(null);
    }

    @Test
    public void stopPeriodicRecordingNoneMode() {
        mImpressionsManager = getNoneModeManager();

        when(mTaskExecutor.schedule(any(ImpressionsCountRecorderTask.class), eq(0L), eq(1800L), eq(null))).thenReturn("id_2");
        when(mTaskExecutor.schedule(any(UniqueKeysRecorderTask.class), eq(0L), eq(500L), eq(null))).thenReturn("id_3");

        mImpressionsManager.startPeriodicRecording();
        mImpressionsManager.stopPeriodicRecording();

        verify(mTaskExecutor, times(0)).submit(any(ImpressionsRecorderTask.class), eq(null));
        verify(mTaskExecutor).stopTask("id_2");
        verify(mTaskExecutor).stopTask("id_3");
        verify(mTaskExecutor).stopTask(null);
    }

    @Test
    public void pushImpressionRecordsInTelemetry() {

        mImpressionsManager.pushImpression(new Impression("key",
                "key",
                "split",
                "treatment",
                10000,
                "rule",
                25L,
                Collections.emptyMap()));

        verify(mTelemetryRuntimeProducer).recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 1);
    }

    private Impression createImpression() {
        return new Impression("key", "bkey", "split", "on",
                100L, "default rule", 999L, null);
    }

    private Impression createUniqueImpression() {
        return new Impression("key", "bkey", UUID.randomUUID().toString(), "on",
                100L, "default rule", 999L, null);
    }

    @NonNull
    private ImpressionManagerImpl getDebugModeManager() {
        return new ImpressionManagerImpl(mTaskExecutor, mTaskFactory, mTelemetryRuntimeProducer,
                mImpressionsStorage, mUniqueKeysTracker,
                new ImpressionManagerImpl.ImpressionManagerConfig(
                        1800,
                        1800,
                        ImpressionsMode.DEBUG,
                        3,
                        2048,
                        500
                ));
    }

    @NonNull
    private ImpressionManagerImpl getNoneModeManager() {
        return new ImpressionManagerImpl(mTaskExecutor, mTaskFactory, mTelemetryRuntimeProducer,
                mImpressionsStorage, mUniqueKeysTracker,
                new ImpressionManagerImpl.ImpressionManagerConfig(
                        1800,
                        1800,
                        ImpressionsMode.NONE,
                        3,
                        2048,
                        500
                ));
    }
}
