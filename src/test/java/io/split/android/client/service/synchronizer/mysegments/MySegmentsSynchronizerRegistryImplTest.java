package io.split.android.client.service.synchronizer.mysegments;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.service.synchronizer.mysegments.MySegmentsSynchronizerRegistry.Tasks.SegmentType;

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
        mRegistry.loadMySegmentsFromCache(SegmentType.SEGMENT);

        verify(syncMock).loadMySegmentsFromCache();
    }

    @Test
    public void synchronizeMySegmentsGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.synchronizeMySegments(SegmentType.SEGMENT);

        verify(syncMock).synchronizeMySegments();
    }

    @Test
    public void forceMySegmentsSyncGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.forceMySegmentsSync(SegmentType.SEGMENT);

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
        mRegistry.scheduleSegmentsSyncTask(SegmentType.SEGMENT);

        verify(syncMock).scheduleSegmentsSyncTask();
    }

    @Test
    public void submitMySegmentsLoadingTaskGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.submitMySegmentsLoadingTask(SegmentType.SEGMENT);

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
        mRegistry.loadMySegmentsFromCache(SegmentType.SEGMENT);
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock2);

        verify(syncMock2).loadMySegmentsFromCache();
    }

    @Test
    public void callSynchronizeMySegmentsForNewlyRegisteredSyncIfNecessary() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.synchronizeMySegments(SegmentType.SEGMENT);
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock2);

        verify(syncMock2).synchronizeMySegments();
    }

    @Test
    public void callScheduleSegmentsSyncTaskForNewlyRegisteredSyncIfNecessary() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.scheduleSegmentsSyncTask(SegmentType.SEGMENT);
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock2);

        verify(syncMock2).scheduleSegmentsSyncTask();
    }
}
