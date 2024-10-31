package io.split.android.client.service.synchronizer.mysegments;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.service.mysegments.MySegmentUpdateParams;

public class MySegmentsSynchronizerRegistryImplTest {

    private MySegmentsSynchronizerRegistryImpl mRegistry;

    @Before
    public void setUp() {
        mRegistry = new MySegmentsSynchronizerRegistryImpl();
    }

    @Test
    public void loadMySegmentsFromCacheGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.loadMySegmentsFromCache();

        verify(syncMock).loadMySegmentsFromCache();
    }

    @Test
    public void synchronizeMySegmentsGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.synchronizeMySegments();

        verify(syncMock).synchronizeMySegments();
    }

    @Test
    public void forceMySegmentsSyncGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentUpdateParams params = new MySegmentUpdateParams(4L, 1L, 2L);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.forceMySegmentsSync(params);

        verify(syncMock).forceMySegmentsSync(params);
    }

    @Test
    public void destroyGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.destroy();

        verify(syncMock).destroy();
    }

    @Test
    public void scheduleSegmentsSyncTaskGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.scheduleSegmentsSyncTask();

        verify(syncMock).scheduleSegmentsSyncTask();
    }

    @Test
    public void submitMySegmentsLoadingTaskGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.submitMySegmentsLoadingTask();

        verify(syncMock).submitMySegmentsLoadingTask();
    }

    @Test
    public void stopPeriodicFetchingGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.stopPeriodicFetching();

        verify(syncMock).stopPeriodicFetching();
    }

    @Test
    public void unregisterStopsTasksBeforeRemovingSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.unregisterMySegmentsSynchronizer("key");

        verify(syncMock).stopPeriodicFetching();
        verify(syncMock).destroy();
    }

    @Test
    public void callLoadSegmentsFromCacheForNewlyRegisteredSyncIfNecessary() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock3 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.loadMySegmentsFromCache();
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock2);
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock3);

        verify(syncMock2).loadMySegmentsFromCache();
        verify(syncMock3).loadMySegmentsFromCache();
    }

    @Test
    public void callSynchronizeMySegmentsForNewlyRegisteredSyncIfNecessary() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock3 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.synchronizeMySegments();
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock2);
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock3);

        verify(syncMock2).synchronizeMySegments();
        verify(syncMock3).synchronizeMySegments();
    }

    @Test
    public void callScheduleSegmentsSyncTaskForNewlyRegisteredSyncIfNecessary() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock3 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.scheduleSegmentsSyncTask();
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock2);
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock3);

        verify(syncMock2).scheduleSegmentsSyncTask();
        verify(syncMock3).scheduleSegmentsSyncTask();
    }
}
