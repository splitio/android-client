package io.split.android.client.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.helpers.FileHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SplitUpdateTaskTest {

    SplitsStorage mSplitsStorage;
    SplitChange mSplitChange = null;
    SplitsSyncHelper mSplitsSyncHelper;
    SplitEventsManager mEventsManager;

    SplitsUpdateTask mTask;

    long mChangeNumber = 234567833L;

    @Before
    public void setup() {
        mSplitsStorage = Mockito.mock(SplitsStorage.class);
        mSplitsSyncHelper = Mockito.mock(SplitsSyncHelper.class);
        mEventsManager = Mockito.mock(SplitEventsManager.class);
        mTask = new SplitsUpdateTask(mSplitsSyncHelper, mSplitsStorage, mChangeNumber, mEventsManager);
        when(mSplitsSyncHelper.sync(anyLong(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
        when(mSplitsSyncHelper.sync(anyLong())).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
        loadSplitChanges();
    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        when(mSplitsStorage.getTill()).thenReturn(-1L);

        mTask.execute();

        verify(mSplitsSyncHelper).sync(mChangeNumber);
    }

    @Test
    public void storedChangeNumBigger() throws HttpFetcherException {
        when(mSplitsStorage.getTill()).thenReturn(mChangeNumber + 100L);

        mTask.execute();

        verify(mSplitsSyncHelper, never()).sync(anyLong(), anyBoolean(), anyBoolean(), anyBoolean());
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
