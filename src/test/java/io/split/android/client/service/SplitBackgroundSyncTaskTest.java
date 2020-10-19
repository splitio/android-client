package io.split.android.client.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.splits.SplitsSyncBackgroundTask;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.helpers.FileHelper;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SplitBackgroundSyncTaskTest {

    SplitsStorage mSplitsStorage;
    SplitChange mSplitChange = null;
    SplitsSyncHelper mSplitsSyncHelper;

    SplitsSyncBackgroundTask mTask;

    Map<String, Object> mDefaultParams = new HashMap<>();

    long mChangeNumber = 234567833L;

    @Before
    public void setup() {
        mDefaultParams.clear();
        mDefaultParams.put("since", -1L);
        mSplitsStorage = Mockito.mock(SplitsStorage.class);
        mSplitsSyncHelper = Mockito.mock(SplitsSyncHelper.class);
        loadSplitChanges();
    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsSyncHelper.cacheHasExpired(anyLong(), anyLong(), anyLong())).thenReturn(false);
        mTask = new SplitsSyncBackgroundTask(mSplitsSyncHelper, mSplitsStorage, 10000L);

        mTask.execute();

        verify(mSplitsSyncHelper, times(1)).sync(mDefaultParams, false);
        verify(mSplitsStorage, never()).clear();
    }

    @Test
    public void correctExecutionExpiredCache() throws HttpFetcherException {

        when(mSplitsStorage.getTill()).thenReturn(10L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(100L);
        when(mSplitsSyncHelper.cacheHasExpired(anyLong(), anyLong(), anyLong())).thenReturn(true);
        mTask = new SplitsSyncBackgroundTask(mSplitsSyncHelper, mSplitsStorage, 10000L);

        mTask.execute();

        verify(mSplitsSyncHelper, times(1)).sync(mDefaultParams, true);
    }


    @After
    public void tearDown() {
        reset(mSplitsStorage);
    }

    private void loadSplitChanges() {
        if (mSplitChange == null) {
            FileHelper fileHelper = new FileHelper();
            mSplitChange = fileHelper.loadSplitChangeFromFile("split_changes_1.json");
        }
    }
}
