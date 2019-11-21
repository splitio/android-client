package io.split.android.client.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.splits.SplitFetcherV2;
import io.split.android.client.storage.SplitStorageProvider;
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
    SplitStorageProvider mSplitStorageProvider;
    SplitClientConfig mSplitClientConfig;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SplitFetcherV2 splitsFetcher = Mockito.mock(SplitFetcherV2.class);
        SplitsStorage splitsStorage = Mockito.mock(SplitsStorage.class);
        when(mSplitApiFacade.getSplitFetcher()).thenReturn(splitsFetcher);
        when(mSplitStorageProvider.getSplitStorage()).thenReturn(splitsStorage);
        mSplitClientConfig = SplitClientConfig.builder().build();
        mSyncManager = new SyncManagerImpl(mSplitClientConfig, mTaskExecutor, mSplitApiFacade, mSplitStorageProvider);
    }

    @Test
    public void schedule() {
        mSyncManager.start();
        verify(mTaskExecutor, times(1)).schedule(any(SplitsSyncTask.class), anyLong(), anyLong());
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
