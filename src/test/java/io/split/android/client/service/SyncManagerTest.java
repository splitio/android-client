package io.split.android.client.service;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.impressions.Impression;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SyncManagerTest {

    SyncManager mSyncManager;
    @Mock
    SplitTaskExecutor mTaskExecutor;
    @Mock
    SplitApiFacade mSplitApiFacade;
    @Mock
    SplitStorageContainer mSplitStorageContainer;
    @Mock
    PersistentEventsStorage mEventsStorage;
    @Mock
    PersistentImpressionsStorage mImpressionsStorage;
    @Mock
    SplitTaskExecutionListener mTaskExecutionListener;
    @Mock
    WorkManager mWorkManager;
    @Mock
    SplitTaskFactory mTaskFactory;

    public void setup(SplitClientConfig splitClientConfig) {
        MockitoAnnotations.initMocks(this);
        HttpFetcher<SplitChange> splitsFetcher = Mockito.mock(HttpFetcher.class);
        HttpFetcher<List<MySegment>> mySegmentsFetcher = Mockito.mock(HttpFetcher.class);
        HttpRecorder<List<Event>> eventsRecorder = Mockito.mock(HttpRecorder.class);
        HttpRecorder<List<KeyImpression>> impressionsRecorder = Mockito.mock(HttpRecorder.class);

        SplitsStorage splitsStorage = Mockito.mock(SplitsStorage.class);
        MySegmentsStorage mySegmentsStorage = Mockito.mock(MySegmentsStorage.class);

        when(mSplitApiFacade.getSplitFetcher()).thenReturn(splitsFetcher);
        when(mSplitApiFacade.getMySegmentsFetcher()).thenReturn(mySegmentsFetcher);
        when(mSplitApiFacade.getEventsRecorder()).thenReturn(eventsRecorder);
        when(mSplitApiFacade.getImpressionsRecorder()).thenReturn(impressionsRecorder);

        when(mSplitStorageContainer.getSplitsStorage()).thenReturn(splitsStorage);
        when(mSplitStorageContainer.getMySegmentsStorage()).thenReturn(mySegmentsStorage);
        when(mSplitStorageContainer.getEventsStorage()).thenReturn(mEventsStorage);
        when(mSplitStorageContainer.getImpressionsStorage()).thenReturn(mImpressionsStorage);



        mSyncManager = new SyncManagerImpl(splitClientConfig, mTaskExecutor,
                mSplitStorageContainer, mTaskFactory, mWorkManager);
    }

    @Test
    public void schedule() throws InterruptedException {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSyncManager.start();
        verify(mTaskExecutor, times(1)).schedule(
                any(SplitsSyncTask.class), anyLong(), anyLong(), mTaskExecutionListener);
        verify(mTaskExecutor, times(1)).schedule(
                any(MySegmentsSyncTask.class), anyLong(), anyLong(), mTaskExecutionListener);
        verify(mTaskExecutor, times(1)).schedule(
                any(EventsRecorderTask.class), anyLong(), anyLong(), mTaskExecutionListener);
        verify(mTaskExecutor, times(1)).schedule(
                any(ImpressionsRecorderTask.class), anyLong(), anyLong(), mTaskExecutionListener);

        verify(mWorkManager, never()).enqueueUniquePeriodicWork(
                SplitTaskType.SPLITS_SYNC.toString(), any(ExistingPeriodicWorkPolicy))
    }

    @Test
    public void pause() {
        SplitClientConfig config = SplitClientConfig.builder()
                .eventsQueueSize(10)
                .sychronizeInBackground(false)
                .impressionsQueueSize(3)
                .build();
        setup(config);
        mSyncManager.start();
        mSyncManager.pause();
        verify(mTaskExecutor, times(1)).pause();
    }

    @Test
    public void resume() {
        mSyncManager.start();
        mSyncManager.pause();
        mSyncManager.resume();
        verify(mTaskExecutor, times(1)).resume();
    }

    @Test
    public void pushEvent() {
        Event event = new Event();
        mSyncManager.start();
        mSyncManager.pushEvent(event);
        verify(mTaskExecutor, times(0)).submit(any(SplitTask.class));
        verify(mEventsStorage, times(1)).push(event);
    }

    @Test
    public void pushEventReachQueueSize() {
        mSyncManager.start();
        for (int i = 0; i < 22; i++) {
            mSyncManager.pushEvent(new Event());
        }

        verify(mEventsStorage, times(22)).push(any(Event.class));
        verify(mTaskExecutor, times(2)).submit(any(SplitTask.class));
    }

    @Test
    public void pushEventBytesLimit() {
        mSyncManager.start();
        for (int i = 0; i < 6; i++) {
            Event event = new Event();
            event.setSizeInBytes(2000000);
            mSyncManager.pushEvent(event);
        }

        verify(mEventsStorage, times(6)).push(any(Event.class));
        verify(mTaskExecutor, times(2)).submit(any(SplitTask.class));
    }

    @Test
    public void pushImpression() {
        Impression impression = createImpression();
        ArgumentCaptor<KeyImpression> impressionCaptor = ArgumentCaptor.forClass(KeyImpression.class);
        mSyncManager.start();
        mSyncManager.pushImpression(impression);
        verify(mTaskExecutor, times(0)).submit(any(SplitTask.class));
        verify(mImpressionsStorage, times(1)).push(impressionCaptor.capture());
        Assert.assertEquals("key", impressionCaptor.getValue().keyName);
        Assert.assertEquals("bkey", impressionCaptor.getValue().bucketingKey);
        Assert.assertEquals("split", impressionCaptor.getValue().feature);
        Assert.assertEquals("on", impressionCaptor.getValue().treatment);
        Assert.assertEquals(100L, impressionCaptor.getValue().time);
        Assert.assertEquals("default rule", impressionCaptor.getValue().label);
        Assert.assertEquals(999, impressionCaptor.getValue().changeNumber.longValue());
    }

    @Test
    public void pushImpressionReachQueueSize() {
        mSyncManager.start();
        for (int i = 0; i < 8; i++) {
            mSyncManager.pushImpression(createImpression());
        }

        verify(mImpressionsStorage, times(8)).push(any(KeyImpression.class));
        verify(mTaskExecutor, times(2)).submit(any(SplitTask.class));
    }

    @Test
    public void pushImpressionBytesLimit() {

        mSyncManager.start();
        for (int i = 0; i < 10; i++) {
            mSyncManager.pushImpression(createImpression());
        }

        verify(mImpressionsStorage, times(10)).push(any(KeyImpression.class));
        verify(mTaskExecutor, times(2)).submit(any(SplitTask.class));
    }

    @After
    public void tearDown() {
    }

    private Impression createImpression() {
        return new Impression("key", "bkey", "split", "on",
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
}
