package io.split.android.client.service.impressions.observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;

public class PeriodicPersistenceTaskTest {

    private Map<Long, Long> mCache;
    private ImpressionsObserverCacheDao mImpressionsObserverCacheDao;
    private PeriodicPersistenceTask.OnExecutedListener mOnExecutedListener;

    @Before
    public void setUp() {
        mCache = new HashMap<>();
        mImpressionsObserverCacheDao = mock(ImpressionsObserverCacheDao.class);
        mOnExecutedListener = mock(PeriodicPersistenceTask.OnExecutedListener.class);
    }

    @Test
    public void noInteractionsWithDaoWhenDelayedSyncRunningIsFalse() {
        PeriodicPersistenceTask task = new PeriodicPersistenceTask(mCache, mImpressionsObserverCacheDao, mOnExecutedListener);
        task.run();

        verify(mImpressionsObserverCacheDao, times(0)).insert(any(), any(), any());
    }

    @Test
    public void valuesFromCacheArePersisted() {
        mCache.put(1L, 1L);
        mCache.put(2L, 2L);

        PeriodicPersistenceTask task = new PeriodicPersistenceTask(mCache, mImpressionsObserverCacheDao, mOnExecutedListener);
        task.run();

        verify(mImpressionsObserverCacheDao).insert(eq(1L), eq(1L), anyLong());
        verify(mImpressionsObserverCacheDao).insert(eq(2L), eq(2L), anyLong());
    }

    @Test
    public void cacheIsCleared() {
        mCache.put(1L, 1L);
        mCache.put(2L, 2L);

        PeriodicPersistenceTask task = new PeriodicPersistenceTask(mCache, mImpressionsObserverCacheDao, mOnExecutedListener);
        task.run();

        assertEquals(0, mCache.size());
    }

    @Test
    public void callbackIsExecutedWhenTaskFinished() {
        PeriodicPersistenceTask task = new PeriodicPersistenceTask(mCache, mImpressionsObserverCacheDao, mOnExecutedListener);
        task.run();

        verify(mOnExecutedListener).onExecuted();
    }

    @Test
    public void nullCacheDoesNotThrow() {
        PeriodicPersistenceTask task = new PeriodicPersistenceTask(null, mImpressionsObserverCacheDao, mOnExecutedListener);
        task.run();
    }

    @Test
    public void callbackIsNotExecutedWhenItIsNull() {
        PeriodicPersistenceTask task = new PeriodicPersistenceTask(mCache, mImpressionsObserverCacheDao, null);
        task.run();
    }

    @Test
    public void exceptionInInsertDoesNotThrow() {
        doAnswer(invocation -> {
            throw new RuntimeException();
        }).when(mImpressionsObserverCacheDao).insert(eq(1L), eq(1L), any());

        mCache.put(1L, 1L);
        mCache.put(2L, 2L);

        PeriodicPersistenceTask task = new PeriodicPersistenceTask(mCache, mImpressionsObserverCacheDao, mOnExecutedListener);
        task.run();

        verify(mImpressionsObserverCacheDao).insert(eq(2L), eq(2L), any());
    }
}
