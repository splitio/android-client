package io.split.android.client.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitFetcherV2;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.helpers.FileHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SplitSyncTaskTest {

    SplitFetcherV2 mSplitsFetcher;
    SplitsStorage mSplitsStorage;
    SplitChange mSplitChange = null;
    SplitChangeProcessor mSplitChangeProcessor;

    SplitsSyncTask mTask;

    @Before
    public void setup() {
        mSplitsFetcher = Mockito.mock(SplitFetcherV2.class);
        mSplitsStorage = Mockito.mock(SplitsStorage.class);
        mSplitChangeProcessor = Mockito.spy(SplitChangeProcessor.class);
        mTask = new SplitsSyncTask(mSplitsFetcher, mSplitsStorage, mSplitChangeProcessor);
        loadSplitChanges();
    }

    @Test
    public void correctExecution() {
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsFetcher.execute(-1)).thenReturn(mSplitChange);

        mTask.execute();

        verify(mSplitsFetcher, times(1)).execute(-1);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);
    }

    @Test
    public void fetcherException() {
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsFetcher.execute(-1)).thenThrow(IllegalStateException.class);

        mTask.execute();

        verify(mSplitsFetcher, times(1)).execute(-1);
        verify(mSplitsStorage, never()).update(any());
        verify(mSplitChangeProcessor, never()).process(mSplitChange);
    }

    @Test
    public void storageException() {
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsFetcher.execute(-1)).thenReturn(mSplitChange);
        doThrow(NullPointerException.class).when(mSplitsStorage).update(any(ProcessedSplitChange.class));

        mTask.execute();

        verify(mSplitsFetcher, times(1)).execute(-1);
        verify(mSplitsStorage, times(1)).update(any());
        verify(mSplitChangeProcessor, times(1)).process(mSplitChange);
    }

    @After
    public void tearDown() {
        reset(mSplitsFetcher);
        reset(mSplitsStorage);
    }

    private void loadSplitChanges() {
        if (mSplitChange == null) {
            FileHelper fileHelper = new FileHelper();
            mSplitChange = fileHelper.loadSplitChangeFromFile("split_changes_1.json");
        }
    }
}
