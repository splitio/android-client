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
    public void loadMySegmentsFromCacheGetCalledInEveryRegisteredSyncForLargeSegments() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMyLargeSegmentsSynchronizer("key", syncMock);
        mRegistry.loadMySegmentsFromCache(SegmentType.LARGE_SEGMENT);

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
    public void synchronizeMySegmentsGetCalledInEveryRegisteredSyncForLargeSegments() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMyLargeSegmentsSynchronizer("key", syncMock);
        mRegistry.synchronizeMySegments(SegmentType.LARGE_SEGMENT);

        verify(syncMock).synchronizeMySegments();
    }

    @Test
    public void forceMySegmentsSyncGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.forceMySegmentsSync(SegmentType.SEGMENT, 4L);

        verify(syncMock).forceMySegmentsSync(4L);
    }

    @Test
    public void forceMySegmentsSyncGetCalledInEveryRegisteredSyncForLargeSegments() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMyLargeSegmentsSynchronizer("key", syncMock);
        mRegistry.forceMySegmentsSync(SegmentType.LARGE_SEGMENT, 3L);

        verify(syncMock).forceMySegmentsSync(3L);
    }

    @Test
    public void destroyGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.registerMyLargeSegmentsSynchronizer("key", syncMock2);
        mRegistry.destroy();

        verify(syncMock).destroy();
        verify(syncMock2).destroy();
    }

    @Test
    public void scheduleSegmentsSyncTaskGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.scheduleSegmentsSyncTask(SegmentType.SEGMENT);

        verify(syncMock).scheduleSegmentsSyncTask();
    }

    @Test
    public void scheduleSegmentsSyncTaskGetCalledInEveryRegisteredSyncForLargeSegments() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMyLargeSegmentsSynchronizer("key", syncMock);
        mRegistry.scheduleSegmentsSyncTask(SegmentType.LARGE_SEGMENT);

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
    public void submitMySegmentsLoadingTaskGetCalledInEveryRegisteredSyncForLargeSegments() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMyLargeSegmentsSynchronizer("key", syncMock);
        mRegistry.submitMySegmentsLoadingTask(SegmentType.LARGE_SEGMENT);

        verify(syncMock).submitMySegmentsLoadingTask();
    }

    @Test
    public void stopPeriodicFetchingGetCalledInEveryRegisteredSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.registerMyLargeSegmentsSynchronizer("key", syncMock2);
        mRegistry.stopPeriodicFetching();

        verify(syncMock).stopPeriodicFetching();
        verify(syncMock2).stopPeriodicFetching();
    }

    @Test
    public void unregisterStopsTasksBeforeRemovingSync() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.registerMyLargeSegmentsSynchronizer("key", syncMock2);
        mRegistry.unregisterMySegmentsSynchronizer("key");

        verify(syncMock).stopPeriodicFetching();
        verify(syncMock2).stopPeriodicFetching();
        verify(syncMock).destroy();
        verify(syncMock2).destroy();
    }

    @Test
    public void callLoadSegmentsFromCacheForNewlyRegisteredSyncIfNecessary() {
        MySegmentsSynchronizer syncMock = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock2 = mock(MySegmentsSynchronizer.class);
        MySegmentsSynchronizer syncMock3 = mock(MySegmentsSynchronizer.class);

        mRegistry.registerMySegmentsSynchronizer("key", syncMock);
        mRegistry.loadMySegmentsFromCache(SegmentType.SEGMENT);
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
        mRegistry.synchronizeMySegments(SegmentType.SEGMENT);
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
        mRegistry.scheduleSegmentsSyncTask(SegmentType.SEGMENT);
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock2);
        mRegistry.registerMySegmentsSynchronizer("new_key", syncMock3);

        verify(syncMock2).scheduleSegmentsSyncTask();
        verify(syncMock3).scheduleSegmentsSyncTask();
    }
}
