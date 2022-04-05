package io.split.android.client.service.synchronizer.mysegments;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

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

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.forceMySegmentsSync();

        verify(syncMock).forceMySegmentsSync();
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

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.loadMySegmentsFromCache();
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock2);

        verify(syncMock2).loadMySegmentsFromCache();
    }

    @Test
    public void callSynchronizeMySegmentsForNewlyRegisteredSyncIfNecessary() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.synchronizeMySegments();
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock2);

        verify(syncMock2).synchronizeMySegments();
    }

    @Test
    public void callScheduleSegmentsSyncTaskForNewlyRegisteredSyncIfNecessary() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.scheduleSegmentsSyncTask();
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock2);

        verify(syncMock2).scheduleSegmentsSyncTask();
    }
}
