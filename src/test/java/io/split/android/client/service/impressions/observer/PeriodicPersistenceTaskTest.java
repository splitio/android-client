package io.split.android.client.service.impressions.observer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheDao;
import io.split.android.client.storage.db.impressions.observer.ImpressionsObserverCacheEntity;

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

        verify(mImpressionsObserverCacheDao, times(0)).insert(anyList());
    }

    @Test
    public void valuesFromCacheArePersisted() {
        mCache.put(1L, 1L);
        mCache.put(2L, 2L);

        PeriodicPersistenceTask task = new PeriodicPersistenceTask(mCache, mImpressionsObserverCacheDao, mOnExecutedListener);
        task.run();

        verify(mImpressionsObserverCacheDao).insert(argThat(new ArgumentMatcher<List<ImpressionsObserverCacheEntity>>() {
            @Override
            public boolean matches(List<ImpressionsObserverCacheEntity> argument) {
                return argument.size() == 2 && (argument.get(0).getHash() == 1L && argument.get(0).getTime() == 1L
                        && argument.get(1).getHash() == 2L && argument.get(1).getTime() == 2L ||
                        argument.get(1).getHash() == 1L && argument.get(1).getTime() == 1L
                                && argument.get(0).getHash() == 2L && argument.get(0).getTime() == 2L);
            }
        }));
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
        }).when(mImpressionsObserverCacheDao).insert(any());

        mCache.put(1L, 1L);
        mCache.put(2L, 2L);

        PeriodicPersistenceTask task = new PeriodicPersistenceTask(mCache, mImpressionsObserverCacheDao, mOnExecutedListener);
        task.run();

        verify(mImpressionsObserverCacheDao).insert(argThat(new ArgumentMatcher<List<ImpressionsObserverCacheEntity>>() {
            @Override
            public boolean matches(List<ImpressionsObserverCacheEntity> argument) {
                return argument.size() == 2;
            }
        }));
    }
}
