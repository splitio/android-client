package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.split.android.client.RetryBackoffCounterTimerFactory;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.impressions.ImpressionManager;
import io.split.android.client.service.impressions.ImpressionManagerConfig;
import io.split.android.client.service.impressions.ImpressionsCountRecorderTask;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.impressions.SaveImpressionsCountTask;
import io.split.android.client.service.mysegments.LoadMySegmentsTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsTaskFactory;
import io.split.android.client.service.splits.FilterSplitsInCacheTask;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.service.sseclient.sseclient.RetryBackoffCounterTimer;
import io.split.android.client.service.synchronizer.FeatureFlagsSynchronizer;
import io.split.android.client.service.synchronizer.RecorderSyncHelper;
import io.split.android.client.service.synchronizer.SynchronizerImpl;
import io.split.android.client.service.synchronizer.WorkManagerWrapper;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizer;
import io.split.android.client.service.synchronizer.attributes.AttributesSynchronizerRegistryImpl;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizer;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistry;
import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistryImpl;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.storage.events.EventsStorage;
import io.split.android.client.storage.impressions.ImpressionsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.fake.SplitTaskExecutorStub;

public class SynchronizerTest {

    private SynchronizerImpl mSynchronizer;

    SplitTaskExecutor mTaskExecutor;
    @Mock
    private SplitTaskExecutor mSingleThreadedTaskExecutor;
    @Mock
    private SplitApiFacade mSplitApiFacade;
    @Mock
    private SplitStorageContainer mSplitStorageContainer;
    @Mock
    private PersistentSplitsStorage mPersistentSplitsStorageContainer;
    @Mock
    private EventsStorage mEventsStorage;
    @Mock
    private ImpressionsStorage mImpressionsStorage;
    @Mock
    private TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    @Mock
    private RetryBackoffCounterTimerFactory mRetryBackoffFactory;

    @Mock
    private RetryBackoffCounterTimer mRetryTimerSplitsSync;

    @Mock
    private RetryBackoffCounterTimer mRetryTimerSplitsUpdate;

    @Mock
    private RetryBackoffCounterTimer mRetryTimerMySegmentsSync;

    @Mock
    private RetryBackoffCounterTimer mRetryTimerEventsRecorder;

    @Mock
    private WorkManager mWorkManager;
    @Mock
    private SplitTaskFactory mTaskFactory;
    @Mock
    private SplitEventsManager mEventsManager;
    @Mock
    private WorkManagerWrapper mWorkManagerWrapper;
    @Mock
    private MySegmentsTaskFactory mMySegmentsTaskFactory;
    @Mock
    private MySegmentsSynchronizer mMySegmentsSynchronizer;
    @Mock
    private MySegmentsSynchronizerRegistryImpl mMySegmentsSynchronizerRegistry;
    @Mock
    private AttributesSynchronizerRegistryImpl mAttributesSynchronizerRegistry;
    @Mock
    private FeatureFlagsSynchronizer mFeatureFlagsSynchronizer;
    ImpressionManager mImpressionManager;

    private final String mUserKey = "user_key";

    public void setup(SplitClientConfig splitClientConfig) {
        setup(splitClientConfig, ImpressionManagerConfig.Mode.fromImpressionMode(splitClientConfig.impressionsMode()));
    }

    public void setup(SplitClientConfig splitClientConfig, ImpressionManagerConfig.Mode impressionsMode) {
        setup(splitClientConfig, impressionsMode, false);
    }

    public void setup(SplitClientConfig splitClientConfig, ImpressionManagerConfig.Mode impressionsMode, boolean useImpManagerMock) {
        MockitoAnnotations.openMocks(this);

        mTaskExecutor = spy(new SplitTaskExecutorStub());
        mSingleThreadedTaskExecutor = spy(new SplitTaskExecutorStub());
        HttpFetcher<SplitChange> splitsFetcher = Mockito.mock(HttpFetcher.class);
        HttpFetcher<List<MySegment>> mySegmentsFetcher = Mockito.mock(HttpFetcher.class);
        HttpRecorder<List<Event>> eventsRecorder = Mockito.mock(HttpRecorder.class);
        HttpRecorder<List<KeyImpression>> impressionsRecorder = Mockito.mock(HttpRecorder.class);

        SplitsStorage splitsStorage = Mockito.mock(SplitsStorage.class);
        MySegmentsStorage mySegmentsStorage = Mockito.mock(MySegmentsStorage.class);

        when(mSplitApiFacade.getSplitFetcher()).thenReturn(splitsFetcher);
        when(mSplitApiFacade.getMySegmentsFetcher(mUserKey)).thenReturn(mySegmentsFetcher);
        when(mSplitApiFacade.getEventsRecorder()).thenReturn(eventsRecorder);
        when(mSplitApiFacade.getImpressionsRecorder()).thenReturn(impressionsRecorder);

        when(mSplitStorageContainer.getSplitsStorage()).thenReturn(splitsStorage);
        when(mSplitStorageContainer.getPersistentSplitsStorage()).thenReturn(mPersistentSplitsStorageContainer);
        when(mSplitStorageContainer.getMySegmentsStorage(mUserKey)).thenReturn(mySegmentsStorage);
        when(mSplitStorageContainer.getEventsStorage()).thenReturn(mEventsStorage);
        when(mSplitStorageContainer.getImpressionsStorage()).thenReturn(mImpressionsStorage);

        when(mTaskFactory.createSplitsSyncTask(anyBoolean())).thenReturn(Mockito.mock(SplitsSyncTask.class));
        when(mMySegmentsTaskFactory.createMySegmentsSyncTask(anyBoolean())).thenReturn(Mockito.mock(MySegmentsSyncTask.class));
        when(mTaskFactory.createImpressionsRecorderTask()).thenReturn(Mockito.mock(ImpressionsRecorderTask.class));
        when(mTaskFactory.createEventsRecorderTask()).thenReturn(Mockito.mock(EventsRecorderTask.class));
        when(mTaskFactory.createLoadSplitsTask(any())).thenReturn(Mockito.mock(LoadSplitsTask.class));
        when(mTaskFactory.createFilterSplitsInCacheTask()).thenReturn(Mockito.mock(FilterSplitsInCacheTask.class));
        when(mTaskFactory.createImpressionsCountRecorderTask()).thenReturn(Mockito.mock(ImpressionsCountRecorderTask.class));
        when(mTaskFactory.createSaveImpressionsCountTask(any())).thenReturn(Mockito.mock(SaveImpressionsCountTask.class));

        when(mRetryBackoffFactory.create(any(), anyInt()))
                .thenReturn(mRetryTimerSplitsSync)
                .thenReturn(mRetryTimerSplitsUpdate);
        when(mRetryBackoffFactory.createWithFixedInterval(any(), eq(1), eq(3)))
                .thenReturn(mRetryTimerEventsRecorder);
        mImpressionManager = Mockito.mock(ImpressionManager.class);

        mSynchronizer = new SynchronizerImpl(splitClientConfig, mTaskExecutor, mSingleThreadedTaskExecutor,
                mTaskFactory, mWorkManagerWrapper, mRetryBackoffFactory, mTelemetryRuntimeProducer, mAttributesSynchronizerRegistry, mMySegmentsSynchronizerRegistry, mImpressionManager, mFeatureFlagsSynchronizer, mSplitStorageContainer.getEventsStorage());
    }

    @Test
    public void splitExecutorSchedule() {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .userConsent(UserConsent.GRANTED)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);

        mSynchronizer.startPeriodicFetching();
        mSynchronizer.startPeriodicRecording();

        verify(mFeatureFlagsSynchronizer).startPeriodicFetching();
        verify(mMySegmentsSynchronizerRegistry).scheduleSegmentsSyncTask();
        verify(mTaskExecutor).schedule(
                any(EventsRecorderTask.class), anyLong(), anyLong(),
                any(SplitTaskExecutionListener.class));
        verify(mImpressionManager).startPeriodicRecording();

        verify(mWorkManager, never()).enqueueUniquePeriodicWork(
                eq(SplitTaskType.SPLITS_SYNC.toString()),
                any(ExistingPeriodicWorkPolicy.class),
                any(PeriodicWorkRequest.class));

        verify(mWorkManager, never()).enqueueUniquePeriodicWork(
                eq(SplitTaskType.MY_SEGMENTS_SYNC.toString()),
                any(ExistingPeriodicWorkPolicy.class),
                any(PeriodicWorkRequest.class));

        verify(mWorkManager, never()).enqueueUniquePeriodicWork(
                eq(SplitTaskType.EVENTS_RECORDER.toString()),
                any(ExistingPeriodicWorkPolicy.class),
                any(PeriodicWorkRequest.class));

        verify(mWorkManager, never()).enqueueUniquePeriodicWork(
                eq(SplitTaskType.IMPRESSIONS_RECORDER.toString()),
                any(ExistingPeriodicWorkPolicy.class),
                any(PeriodicWorkRequest.class));

        verify(mWorkManagerWrapper).removeWork();
        verify(mWorkManagerWrapper, never()).scheduleWork();

        mSynchronizer.unregisterMySegmentsSynchronizer("userKey");
    }

    @Test
    public void workManagerSchedule() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(true)
                .impressionsQueueSize(3)
                .build();
        setup(config);

        verify(mWorkManagerWrapper, times(1)).scheduleWork();
        verify(mWorkManagerWrapper, never()).removeWork();
    }

    @Test
    public void pauseImpOptimized() {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config, ImpressionManagerConfig.Mode.OPTIMIZED);
        mSynchronizer.startPeriodicFetching();
        mSynchronizer.startPeriodicRecording();
        mSynchronizer.pause();
        verify(mTaskExecutor, times(1)).pause();
        verify(mSingleThreadedTaskExecutor, times(1)).pause();
        verify(mImpressionManager).stopPeriodicRecording();
    }

    @Test
    public void pauseImpDebug() {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .impressionsMode("DEBUG")
                .build();
        setup(config);
        mSynchronizer.startPeriodicFetching();
        mSynchronizer.startPeriodicRecording();
        mSynchronizer.pause();
        verify(mTaskExecutor, times(1)).pause();
        verify(mSingleThreadedTaskExecutor, times(1)).pause();
        verify(mTaskExecutor, never()).submit(
                any(SaveImpressionsCountTask.class), isNull());
    }

    @Test
    public void resumeUserConsentGranted() {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .userConsent(UserConsent.GRANTED)
                .impressionsMode("DEBUG")
                .build();
        setup(config, ImpressionManagerConfig.Mode.DEBUG, true);
        mSynchronizer.pause();
        mSynchronizer.resume();
        verify(mImpressionManager, times(1)).startPeriodicRecording();
        verify(mTaskExecutor, times(1)).schedule(any(EventsRecorderTask.class),
                anyLong(), anyLong(), any(RecorderSyncHelper.class));
    }

    @Test
    public void resumeUserConsentDeclined() {
        testResumeUserConsentNone(UserConsent.DECLINED);
    }

    @Test
    public void resumeUserConsentUnknown() {
        testResumeUserConsentNone(UserConsent.UNKNOWN);
    }

    private void testResumeUserConsentNone(UserConsent userConsent) {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .userConsent(userConsent)
                .impressionsMode("DEBUG")
                .build();
        setup(config, ImpressionManagerConfig.Mode.DEBUG, true);
        mSynchronizer.pause();
        mSynchronizer.resume();
        verify(mImpressionManager, never()).startPeriodicRecording();
        verify(mTaskExecutor, never()).schedule(any(EventsRecorderTask.class), anyLong(), isNull());
    }

    @Test
    public void resume() {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSynchronizer.startPeriodicFetching();
        mSynchronizer.startPeriodicRecording();
        mSynchronizer.pause();
        mSynchronizer.resume();
        verify(mTaskExecutor, times(1)).resume();
        verify(mSingleThreadedTaskExecutor, times(1)).resume();
    }

    @Test
    public void pushEvent() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        Event event = new Event();
        mSynchronizer.startPeriodicRecording();
        mSynchronizer.pushEvent(event);
        Thread.sleep(200);
        verify(mTaskExecutor, times(0)).submit(
                any(EventsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
        verify(mEventsStorage, times(1)).push(event);
    }

    @Test
    public void pushEventReachQueueSize() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSynchronizer.startPeriodicRecording();
        for (int i = 0; i < 22; i++) {
            mSynchronizer.pushEvent(new Event());
        }
        Thread.sleep(200);
        verify(mEventsStorage, times(22)).push(any(Event.class));
        verify(mTaskExecutor, times(2)).submit(
                any(EventsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
    }

    @Test
    public void pushEventBytesLimit() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSynchronizer.startPeriodicRecording();
        for (int i = 0; i < 6; i++) {
            Event event = new Event();
            event.setSizeInBytes(2000000);
            mSynchronizer.pushEvent(event);
        }
        Thread.sleep(200);
        verify(mEventsStorage, times(6)).push(any(Event.class));
        verify(mTaskExecutor, times(2)).submit(
                any(EventsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
    }

    @Test
    public void pushImpression() throws InterruptedException {

        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        Impression impression = createImpression();
        ArgumentCaptor<Impression> impressionCaptor = ArgumentCaptor.forClass(Impression.class);
        mSynchronizer.startPeriodicRecording();
        mSynchronizer.pushImpression(impression);
        Thread.sleep(200);
        verify(mTaskExecutor, times(0)).submit(
                any(ImpressionsRecorderTask.class),
                any(SplitTaskExecutionListener.class));
        verify(mImpressionManager).pushImpression(impressionCaptor.capture());
        Assert.assertEquals("key", impressionCaptor.getValue().key());
        Assert.assertEquals("bkey", impressionCaptor.getValue().bucketingKey());
        Assert.assertEquals("split", impressionCaptor.getValue().split());
        Assert.assertEquals("on", impressionCaptor.getValue().treatment());
        Assert.assertEquals(100L, impressionCaptor.getValue().time());
        Assert.assertEquals("default rule", impressionCaptor.getValue().appliedRule());
        Assert.assertEquals(999, impressionCaptor.getValue().changeNumber().longValue());
    }

    @Test
    public void pushImpressionReachQueueSizeImpDebug() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsMode(ImpressionsMode.DEBUG)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSynchronizer.startPeriodicRecording();
        for (int i = 0; i < 8; i++) {
            mSynchronizer.pushImpression(createImpression());
        }
        Thread.sleep(200);
        verify(mImpressionManager, times(8)).pushImpression(any(Impression.class));
    }

    @Test
    public void pushImpressionReachQueueSizeImpOptimized() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsMode(ImpressionsMode.OPTIMIZED)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSynchronizer.startPeriodicRecording();
        for (int i = 0; i < 8; i++) {
            mSynchronizer.pushImpression(createUniqueImpression());
        }
        Thread.sleep(200);
        verify(mImpressionManager, times(8)).pushImpression(any(Impression.class));
    }

    @Test
    public void pushImpressionBytesLimitImpDebug() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .impressionsMode(ImpressionsMode.DEBUG)
                .build();
        setup(config);

        mSynchronizer.startPeriodicRecording();
        for (int i = 0; i < 10; i++) {
            mSynchronizer.pushImpression(createImpression());
        }
        Thread.sleep(200);
        verify(mImpressionManager, times(10)).pushImpression(any(Impression.class));
    }

    @Test
    public void pushImpressionBytesLimitImpOptimized() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .synchronizeInBackground(false)
                .impressionsQueueSize(3)
                .impressionsMode(ImpressionsMode.OPTIMIZED)
                .build();
        setup(config);

        mSynchronizer.startPeriodicRecording();
        for (int i = 0; i < 10; i++) {
            mSynchronizer.pushImpression(createUniqueImpression());
        }
        Thread.sleep(200);
        verify(mImpressionManager, times(10)).pushImpression(any(Impression.class));
    }

    @Test
    public void loadLocalData() {
        SplitClientConfig config = SplitClientConfig.builder()
                .synchronizeInBackground(false)
                .build();
        setup(config);

        List<SplitTaskExecutionInfo> list = new ArrayList<>();
        list.add(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_MY_SYGMENTS));
        list.add(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS));
        list.add(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_ATTRIBUTES));
        SplitTaskExecutor executor = new SplitTaskExecutorSub(list);
        when(mRetryBackoffFactory.create(any(), anyInt()))
                .thenReturn(mRetryTimerSplitsSync)
                .thenReturn(mRetryTimerSplitsUpdate);

        mSynchronizer = new SynchronizerImpl(config, executor, executor,
                mTaskFactory, mWorkManagerWrapper, mRetryBackoffFactory, mTelemetryRuntimeProducer, mAttributesSynchronizerRegistry, mMySegmentsSynchronizerRegistry, mImpressionManager,
                mFeatureFlagsSynchronizer, mSplitStorageContainer.getEventsStorage());

        LoadMySegmentsTask loadMySegmentsTask = mock(LoadMySegmentsTask.class);
        when(loadMySegmentsTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_MY_SYGMENTS));
        when(mMySegmentsTaskFactory.createLoadMySegmentsTask()).thenReturn(loadMySegmentsTask);

        ((MySegmentsSynchronizerRegistry) mSynchronizer).registerMySegmentsSynchronizer("", mMySegmentsSynchronizer);

        mSynchronizer.loadAndSynchronizeSplits();
        mSynchronizer.loadMySegmentsFromCache();
        mSynchronizer.loadAttributesFromCache();
        verify(mFeatureFlagsSynchronizer).loadAndSynchronize();
        verify(mMySegmentsSynchronizerRegistry).loadMySegmentsFromCache();
        verify(mAttributesSynchronizerRegistry).loadAttributesFromCache();
    }

    @Test
    public void loadAndSynchronizeSplits() {
        SplitClientConfig config = SplitClientConfig.builder()
                .synchronizeInBackground(false)
                .build();
        setup(config);

        List<SplitTaskExecutionInfo> list = new ArrayList<>();
        list.add(SplitTaskExecutionInfo.success(SplitTaskType.FILTER_SPLITS_CACHE));
        list.add(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS));
        list.add(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
        SplitTaskExecutor executor = new SplitTaskExecutorSub(list);
        when(mRetryBackoffFactory.create(any(), anyInt()))
                .thenReturn(mRetryTimerSplitsSync)
                .thenReturn(mRetryTimerMySegmentsSync)
                .thenReturn(mRetryTimerSplitsUpdate);

        mSynchronizer = new SynchronizerImpl(config, executor, executor,
                mTaskFactory, mWorkManagerWrapper, mRetryBackoffFactory, mTelemetryRuntimeProducer, mAttributesSynchronizerRegistry, mMySegmentsSynchronizerRegistry, mImpressionManager,
                mFeatureFlagsSynchronizer, mSplitStorageContainer.getEventsStorage());
        mSynchronizer.loadAndSynchronizeSplits();


        verify(mFeatureFlagsSynchronizer).loadAndSynchronize();
    }

    @Test
    public void destroy() {
        SplitClientConfig config = SplitClientConfig.builder()
                .synchronizeInBackground(false)
                .build();
        setup(config);
        ImpressionManager impressionManager = mock(ImpressionManager.class);
        when(mRetryBackoffFactory.create(any(), anyInt()))
                .thenReturn(mRetryTimerSplitsSync)
                .thenReturn(mRetryTimerSplitsUpdate);
        mSynchronizer = new SynchronizerImpl(config, mTaskExecutor, mSingleThreadedTaskExecutor,
                mTaskFactory, mWorkManagerWrapper,
                mRetryBackoffFactory, mTelemetryRuntimeProducer, mAttributesSynchronizerRegistry,
                mMySegmentsSynchronizerRegistry, impressionManager, mFeatureFlagsSynchronizer, mSplitStorageContainer.getEventsStorage());

        mSynchronizer.destroy();

        verify(mFeatureFlagsSynchronizer).stopSynchronization();
        verify(impressionManager).flush();
        verify(mRetryTimerEventsRecorder).setTask(any(EventsRecorderTask.class));
        verify(mRetryTimerEventsRecorder).start();
    }

    @Test
    public void loadMySegmentsFromCacheDelegatesToRegistry() {
        setup(SplitClientConfig.builder().synchronizeInBackground(false).build());

        mSynchronizer.loadMySegmentsFromCache();

        verify(mMySegmentsSynchronizerRegistry).loadMySegmentsFromCache();
    }

    @Test
    public void synchronizeMySegmentsDelegatesToRegistry() {
        setup(SplitClientConfig.builder().synchronizeInBackground(false).build());

        mSynchronizer.synchronizeMySegments();

        verify(mMySegmentsSynchronizerRegistry).synchronizeMySegments();
    }

    @Test
    public void forceMySegmentsSyncDelegatesToRegistry() {
        setup(SplitClientConfig.builder().synchronizeInBackground(false).build());

        mSynchronizer.forceMySegmentsSync();

        verify(mMySegmentsSynchronizerRegistry).forceMySegmentsSync();
    }

    @Test
    public void destroyDelegatesToRegisteredSyncs() {
        setup(SplitClientConfig.builder().synchronizeInBackground(false).build());

        mSynchronizer.destroy();

        verify(mMySegmentsSynchronizerRegistry).destroy();
    }

    @Test
    public void startPeriodicFetchingDelegatesToRegistry() {
        setup(SplitClientConfig.builder().synchronizeInBackground(false).build());

        mSynchronizer.startPeriodicFetching();

        verify(mMySegmentsSynchronizerRegistry).scheduleSegmentsSyncTask();
    }

    @Test
    public void stopPeriodicFetchingDelegatesToRegistry() {
        setup(SplitClientConfig.builder().synchronizeInBackground(false).build());

        mSynchronizer.stopPeriodicFetching();

        verify(mMySegmentsSynchronizerRegistry).stopPeriodicFetching();
    }

    @Test
    public void loadAttributesFromCacheDelegatesToAttributesSynchronizerRegistry() {
        setup(SplitClientConfig.builder().persistentAttributesEnabled(true).build());

        mSynchronizer.loadAttributesFromCache();

        verify(mAttributesSynchronizerRegistry).loadAttributesFromCache();
    }

    @Test
    public void attributesSynchronizerRegistration() {
        setup(SplitClientConfig.builder().persistentAttributesEnabled(true).build());

        AttributesSynchronizer attributesSynchronizer = mock(AttributesSynchronizer.class);
        mSynchronizer.registerAttributesSynchronizer("my_key", attributesSynchronizer);

        verify(mAttributesSynchronizerRegistry).registerAttributesSynchronizer("my_key", attributesSynchronizer);
    }

    @Test
    public void attributesSynchronizerUnregistration() {
        setup(SplitClientConfig.builder().persistentAttributesEnabled(true).build());

        mSynchronizer.unregisterAttributesSynchronizer("my_key");

        verify(mAttributesSynchronizerRegistry).unregisterAttributesSynchronizer("my_key");
    }

    @Test
    public void beingNotifiedOfSplitsSyncTaskTriggersSplitsLoad() {
        setup(SplitClientConfig.builder().persistentAttributesEnabled(false).build());

        LoadSplitsTask task = mock(LoadSplitsTask.class);
        when(mTaskFactory.createLoadSplitsTask(any())).thenReturn(task);

        mSynchronizer.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        verify(mFeatureFlagsSynchronizer).submitLoadingTask(null);
    }

    @Test
    public void beginNotifiedOfMySegmentsSyncTriggersMySegmentsLoad() {
        setup(SplitClientConfig.builder().persistentAttributesEnabled(false).build());

        mSynchronizer.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.MY_SEGMENTS_SYNC));

        verify(mMySegmentsSynchronizerRegistry).submitMySegmentsLoadingTask();
    }

    @Test
    public void synchronizeSplitsWithSince() {
        setup(SplitClientConfig.builder().synchronizeInBackground(false).build());
        SplitsUpdateTask task = mock(SplitsUpdateTask.class);
        when(mTaskFactory.createSplitsUpdateTask(1000)).thenReturn(task);

        mSynchronizer.synchronizeSplits(1000);

        verify(mFeatureFlagsSynchronizer).synchronize(1000);
    }


    @After
    public void tearDown() {
    }

    private Impression createImpression() {
        return new Impression("key", "bkey", "split", "on",
                100L, "default rule", 999L, null);
    }

    private Impression createUniqueImpression() {
        return new Impression("key", "bkey", UUID.randomUUID().toString(), "on",
                100L, "default rule", 999L, null);
    }

    private KeyImpression keyImpression(Impression impression) {
        KeyImpression result = new KeyImpression();
        result.feature = impression.split();
        result.keyName = impression.key();
        result.bucketingKey = impression.bucketingKey();
        result.label = impression.appliedRule();
        result.treatment = impression.treatment();
        result.time = impression.time();
        result.changeNumber = impression.changeNumber();
        return result;
    }

    static class SplitTaskExecutorSub implements SplitTaskExecutor {

        List<SplitTaskExecutionInfo> mInfoList;
        int mRequestIndex = 0;

        public SplitTaskExecutorSub(List<SplitTaskExecutionInfo> infoList) {
            mInfoList = infoList;
        }

        @Override
        public String schedule(@NonNull SplitTask task, long initialDelayInSecs,
                               long periodInSecs,
                               @Nullable SplitTaskExecutionListener executionListener) {
            return UUID.randomUUID().toString();

        }

        @Override
        public void submit(@NonNull SplitTask task,
                           @Nullable SplitTaskExecutionListener executionListener) {

            SplitTaskExecutionInfo info = mInfoList.get(mRequestIndex);
            mRequestIndex++;
            executionListener.taskExecuted(info);
        }

        @Override
        public void executeSerially(List<SplitTaskBatchItem> tasks) {
            for (SplitTaskBatchItem enqueued : tasks) {
                SplitTaskExecutionInfo info = mInfoList.get(mRequestIndex);
                mRequestIndex++;
                enqueued.getTask().execute();
                SplitTaskExecutionListener executionListener = enqueued.getListener();
                if (executionListener != null) {
                    executionListener.taskExecuted(info);
                }
            }
        }

        @Override
        public void pause() {

        }

        @Override
        public void resume() {

        }

        @Override
        public void stopTask(String taskId) {

        }

        @Override
        public void stop() {

        }

        @Override
        public String schedule(@NonNull SplitTask task, long initialDelayInSecs, @Nullable SplitTaskExecutionListener executionListener) {
            return null;
        }

        @Override
        public void submitOnMainThread(SplitTask splitTask) {

        }
    }
}
