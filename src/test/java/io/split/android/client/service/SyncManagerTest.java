package io.split.android.client.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    SplitClientConfig mSplitClientConfig;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        HttpFetcher<SplitChange> splitsFetcher = Mockito.mock(HttpFetcher.class);
        HttpFetcher<List<MySegment>> mySegmentsFetcher = Mockito.mock(HttpFetcher.class);
        HttpRecorder<List<Event>> eventsRecorder = Mockito.mock(HttpRecorder.class);

        SplitsStorage splitsStorage = Mockito.mock(SplitsStorage.class);
        MySegmentsStorage mySegmentsStorage = Mockito.mock(MySegmentsStorage.class);
        PersistentEventsStorage eventsStorage = Mockito.mock(PersistentEventsStorage.class);

        when(mSplitApiFacade.getSplitFetcher()).thenReturn(splitsFetcher);
        when(mSplitApiFacade.getMySegmentsFetcher()).thenReturn(mySegmentsFetcher);
        when(mSplitApiFacade.getEventsRecorder()).thenReturn(eventsRecorder);

        when(mSplitStorageContainer.getSplitsStorage()).thenReturn(splitsStorage);
        when(mSplitStorageContainer.getMySegmentsStorage()).thenReturn(mySegmentsStorage);
        when(mSplitStorageContainer.getEventsStorage()).thenReturn(eventsStorage);

        mSplitClientConfig = SplitClientConfig.builder().build();
        mSyncManager = new SyncManagerImpl(mSplitClientConfig, mTaskExecutor, mSplitApiFacade, mSplitStorageContainer);
    }

    @Test
    public void schedule() throws InterruptedException {
        mSyncManager.start();
        verify(mTaskExecutor, times(1)).schedule(any(SplitsSyncTask.class), anyLong(), anyLong());
        verify(mTaskExecutor, times(1)).schedule(any(MySegmentsSyncTask.class), anyLong(), anyLong());
        verify(mTaskExecutor, times(1)).schedule(any(EventsRecorderTask.class), anyLong(), anyLong());
    }

    @Test
    public void pause() {
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

    @After
    public void tearDown() {
    }
}
