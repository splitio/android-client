package io.split.android.client.service.impressions;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskSerialWrapper;
import io.split.android.client.service.impressions.unique.SaveUniqueImpressionsTask;
import io.split.android.client.service.impressions.unique.UniqueKeysRecorderTask;
import io.split.android.client.service.impressions.unique.UniqueKeysTracker;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;
import io.split.android.client.storage.impressions.ImpressionsStorage;
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
    private ImpressionsStorage mImpressionsStorage;

    @Mock
    private UniqueKeysTracker mUniqueKeysTracker;

    @Mock
    private RecorderSyncHelper<KeyImpression> mRecorderSyncHelper;

    @Mock
    private RetryBackoffCounterTimer mGenericCounterTimer;

    @Mock
    private ImpressionsCounter mImpressionsCounter;

    @Mock
    private ImpressionManagerRetryTimerProvider mImpressionManagerRetryTimerProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mTaskExecutor = spy(new SplitTaskExecutorStub());

        when(mTaskFactory.createImpressionsRecorderTask()).thenReturn(mock(ImpressionsRecorderTask.class));
        when(mTaskFactory.createImpressionsCountRecorderTask()).thenReturn(mock(ImpressionsCountRecorderTask.class));
        when(mTaskFactory.createSaveImpressionsCountTask(any())).thenReturn(mock(SaveImpressionsCountTask.class));
        when(mTaskFactory.createSaveUniqueImpressionsTask(any())).thenReturn(mock(SaveUniqueImpressionsTask.class));
        when(mTaskFactory.createUniqueImpressionsRecorderTask()).thenReturn(mock(UniqueKeysRecorderTask.class));

        ImpressionManagerConfig mConfig = new ImpressionManagerConfig(
                1800,
                1800,
                ImpressionManagerConfig.Mode.OPTIMIZED,
                3,
                2048,
                500);

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
    public void pushImpressionWithNoneModeSavesKeysWhenCacheSizeIsExceeded() {
        when(mUniqueKeysTracker.size()).thenReturn(30000);

        mImpressionsManager = getNoneModeManager();

        mImpressionsManager.pushImpression(createUniqueImpression());

        verify(mTaskExecutor).submit(any(SaveUniqueImpressionsTask.class), eq(null));
    }

    @Test
    public void pushImpressionWhenTrackingDisabled() {

        mImpressionsManager = getDebugModeManager();
        mImpressionsManager.enableTracking(false);

        mImpressionsManager.pushImpression(createImpression());

        verifyNoInteractions(mTelemetryRuntimeProducer);
    }

    @Test
    public void flushWithOptimizedMode() {
        RetryBackoffCounterTimer impressionsTimer = mock(RetryBackoffCounterTimer.class);
        RetryBackoffCounterTimer impressionsCountTimer = mock(RetryBackoffCounterTimer.class);
        RetryBackoffCounterTimer uniqueKeysTimer = mock(RetryBackoffCounterTimer.class);
        when(mImpressionManagerRetryTimerProvider.getImpressionsTimer()).thenReturn(impressionsTimer);
        when(mImpressionManagerRetryTimerProvider.getImpressionsCountTimer()).thenReturn(impressionsCountTimer);
        when(mImpressionManagerRetryTimerProvider.getUniqueKeysTimer()).thenReturn(uniqueKeysTimer);
        mImpressionsManager = getOptimizedModeManager();
        mImpressionsManager.flush();

        verify(impressionsCountTimer).setTask(argThat(new ArgumentMatcher<SplitTaskSerialWrapper>() {
            @Override
            public boolean matches(SplitTaskSerialWrapper argument) {
                List<SplitTask> taskList = argument.getTaskList();
                return taskList.size() == 2 && taskList.get(0) instanceof SaveImpressionsCountTask && taskList.get(1) instanceof ImpressionsCountRecorderTask;
            }
        }));

        verifyNoInteractions(uniqueKeysTimer);
        verify(impressionsCountTimer).start();
    }

    @Test
    public void flushWithDebugMode() {
        RetryBackoffCounterTimer impressionsTimer = mock(RetryBackoffCounterTimer.class);
        RetryBackoffCounterTimer impressionsCountTimer = mock(RetryBackoffCounterTimer.class);
        RetryBackoffCounterTimer uniqueKeysTimer = mock(RetryBackoffCounterTimer.class);
        when(mImpressionManagerRetryTimerProvider.getImpressionsTimer()).thenReturn(impressionsTimer);
        when(mImpressionManagerRetryTimerProvider.getImpressionsCountTimer()).thenReturn(impressionsCountTimer);
        when(mImpressionManagerRetryTimerProvider.getUniqueKeysTimer()).thenReturn(uniqueKeysTimer);
        mImpressionsManager = new ImpressionManagerImpl(mTaskExecutor,
                mTaskFactory,
                mTelemetryRuntimeProducer,
                mImpressionsCounter,
                mUniqueKeysTracker,
                new ImpressionManagerConfig(
                        1800,
                        1800,
                        ImpressionManagerConfig.Mode.DEBUG,
                        3,
                        2048,
                        500
                ),
                mRecorderSyncHelper, mImpressionManagerRetryTimerProvider);

        mImpressionsManager.flush();

        verify(impressionsTimer).setTask(any(ImpressionsRecorderTask.class), any(RecorderSyncHelper.class));

        verify(impressionsTimer).start();
        verifyNoInteractions(impressionsCountTimer);
        verifyNoInteractions(uniqueKeysTimer);
    }

    @Test
    public void flushWithNoneMode() {
        RetryBackoffCounterTimer impressionsTimer = mock(RetryBackoffCounterTimer.class);
        RetryBackoffCounterTimer impressionsCountTimer = mock(RetryBackoffCounterTimer.class);
        RetryBackoffCounterTimer uniqueKeysTimer = mock(RetryBackoffCounterTimer.class);
        when(mImpressionManagerRetryTimerProvider.getImpressionsTimer()).thenReturn(impressionsTimer);
        when(mImpressionManagerRetryTimerProvider.getImpressionsCountTimer()).thenReturn(impressionsCountTimer);
        when(mImpressionManagerRetryTimerProvider.getUniqueKeysTimer()).thenReturn(uniqueKeysTimer);

        mImpressionsManager = new ImpressionManagerImpl(mTaskExecutor,
                mTaskFactory,
                mTelemetryRuntimeProducer,
                mImpressionsCounter,
                mUniqueKeysTracker,
                new ImpressionManagerConfig(
                        1800,
                        1800,
                        ImpressionManagerConfig.Mode.NONE,
                        3,
                        2048,
                        500
                ),
                mRecorderSyncHelper, mImpressionManagerRetryTimerProvider);

        mImpressionsManager.flush();

        verify(impressionsCountTimer).setTask(argThat(new ArgumentMatcher<SplitTaskSerialWrapper>() {
            @Override
            public boolean matches(SplitTaskSerialWrapper argument) {
                List<SplitTask> taskList = argument.getTaskList();
                return taskList.size() == 2 && taskList.get(0) instanceof SaveImpressionsCountTask && taskList.get(1) instanceof ImpressionsCountRecorderTask;
            }
        }));

        verify(uniqueKeysTimer).setTask(argThat(new ArgumentMatcher<SplitTaskSerialWrapper>() {
            @Override
            public boolean matches(SplitTaskSerialWrapper argument) {
                List<SplitTask> taskList = argument.getTaskList();
                return taskList.size() == 2 && taskList.get(0) instanceof SaveUniqueImpressionsTask && taskList.get(1) instanceof UniqueKeysRecorderTask;
            }
        }));

        verify(impressionsTimer).start();
        verify(impressionsCountTimer).start();
        verify(uniqueKeysTimer).start();
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
        when(mImpressionManagerRetryTimerProvider.getUniqueKeysTimer()).thenReturn(mGenericCounterTimer);

        mImpressionsManager = new ImpressionManagerImpl(mTaskExecutor,
                mTaskFactory,
                mTelemetryRuntimeProducer,
                mImpressionsCounter,
                mUniqueKeysTracker,
                new ImpressionManagerConfig(
                        1800,
                        1800,
                        ImpressionManagerConfig.Mode.NONE,
                        3,
                        2048,
                        500
                ),
                mRecorderSyncHelper, mImpressionManagerRetryTimerProvider);

        when(mTaskExecutor.schedule(any(ImpressionsCountRecorderTask.class), eq(0L), eq(1800L), eq(null))).thenReturn("id_2");
        when(mTaskExecutor.schedule(any(UniqueKeysRecorderTask.class), eq(0L), eq(500L), eq(null))).thenReturn("id_3");

        mImpressionsManager.startPeriodicRecording();
        mImpressionsManager.stopPeriodicRecording();

        verify(mTaskExecutor, times(0)).submit(any(ImpressionsRecorderTask.class), eq(null));
        verify(mTaskExecutor).submit(any(SaveImpressionsCountTask.class), eq(null));
        verify(mTaskExecutor).submit(any(SaveUniqueImpressionsTask.class), eq(null));
        verify(mTaskExecutor).stopTask("id_2");
        verify(mTaskExecutor).stopTask("id_3");
        verify(mTaskExecutor).stopTask(null);
    }

    @Test
    public void pushImpressionRecordsInTelemetry() {

        pushDummyImpression(mImpressionsManager);

        verify(mTelemetryRuntimeProducer).recordImpressionStats(ImpressionsDataType.IMPRESSIONS_QUEUED, 1);
    }

    @Test
    public void countIsNotIncrementedWhenPreviousTimeDoesNotExist() {

        mImpressionsManager = getOptimizedModeManager();

        pushDummyImpression(mImpressionsManager);

        verifyNoInteractions(mTaskExecutor);
    }

    @Test
    public void countIsIncrementedWhenPreviousTimeExists() {

        mImpressionsManager = getOptimizedModeManager();

        int impressionsToPush = 3;
        for (int i = 0; i < impressionsToPush; i++) {
            pushDummyImpression(mImpressionsManager);
        }

        verify(mImpressionsCounter, times(impressionsToPush - 1)).inc("split", 10000, 1);
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
                new ImpressionManagerConfig(
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
                new ImpressionManagerConfig(
                        1800,
                        1800,
                        ImpressionManagerConfig.Mode.NONE,
                        3,
                        2048,
                        500
                ));
    }

    @NonNull
    private ImpressionManagerImpl getOptimizedModeManager() {
        when(mImpressionManagerRetryTimerProvider.getUniqueKeysTimer()).thenReturn(mGenericCounterTimer);

        return new ImpressionManagerImpl(mTaskExecutor,
                mTaskFactory,
                mTelemetryRuntimeProducer,
                mImpressionsCounter,
                mUniqueKeysTracker,
                new ImpressionManagerConfig(
                        1800,
                        1800,
                        ImpressionManagerConfig.Mode.OPTIMIZED,
                        3,
                        2048,
                        500
                ),
                mRecorderSyncHelper, mImpressionManagerRetryTimerProvider);
    }

    private static void pushDummyImpression(ImpressionManager impressionsManager) {
        impressionsManager.pushImpression(new Impression("key",
                "key",
                "split",
                "treatment",
                10000,
                "rule",
                25L,
                Collections.emptyMap()));
    }
}
