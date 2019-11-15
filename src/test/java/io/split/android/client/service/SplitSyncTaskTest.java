package io.split.android.client.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitFetcherV2;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.helpers.FileHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SplitSyncTaskTest {
    @Mock
    SplitFetcherV2 mSplitsFetcher;
    @Mock
    SplitsStorage mSplitsStorage;
    @Mock
    SplitChange mSplitChange = null;

    @InjectMocks
    SplitsSyncTask mTask;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        loadSplitChanges();
    }

    @Test
    public void correctExecution() {
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsFetcher.execute(-1)).thenReturn(mSplitChange);

        mTask.execute();

        verify(mSplitsFetcher, times(1)).execute(-1);
        verify(mSplitsStorage, times(1)).update(any());
    }

    @Test
    public void fetcherException() {
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsFetcher.execute(-1)).thenThrow(IllegalStateException.class);

        mTask.execute();

        verify(mSplitsFetcher, times(1)).execute(-1);
        verify(mSplitsStorage, never()).update(any());
    }

    @Test
    public void storageException() {
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsFetcher.execute(-1)).thenReturn(mSplitChange);
        doThrow(NullPointerException.class).when(mSplitsStorage).update(any(ProcessedSplitChange.class));

        mTask.execute();

        verify(mSplitsFetcher, times(1)).execute(-1);
        verify(mSplitsStorage, times(1)).update(any());
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
