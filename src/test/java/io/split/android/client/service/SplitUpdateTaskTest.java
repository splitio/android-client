package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.helpers.FileHelper;

public class SplitUpdateTaskTest {

    SplitsStorage mSplitsStorage;
    RuleBasedSegmentStorage mRuleBasedSegmentStorage;
    SplitChange mSplitChange = null;
    SplitsSyncHelper mSplitsSyncHelper;
    SplitEventsManager mEventsManager;

    SplitsUpdateTask mTask;

    long mChangeNumber = 234567833L;
    long mRbsChangeNumber = 234567830L;

    @Before
    public void setup() {
        mSplitsStorage = Mockito.mock(SplitsStorage.class);
        mRuleBasedSegmentStorage = Mockito.mock(RuleBasedSegmentStorage.class);
        mSplitsSyncHelper = Mockito.mock(SplitsSyncHelper.class);
        mEventsManager = Mockito.mock(SplitEventsManager.class);
        mTask = new SplitsUpdateTask(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage, mChangeNumber, mRbsChangeNumber, mEventsManager);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
        when(mSplitsSyncHelper.sync(any(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
        loadSplitChanges();
    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mRuleBasedSegmentStorage.getChangeNumber()).thenReturn(10L);

        mTask.execute();

        verify(mSplitsSyncHelper).sync(argThat(new ArgumentMatcher<SplitsSyncHelper.SinceChangeNumbers>() {
            @Override
            public boolean matches(SplitsSyncHelper.SinceChangeNumbers argument) {
                return argument.getFlagsSince() == 234567833L && argument.getRbsSince() == 234567830L;
            }
        }), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES));
    }

    @Test
    public void storedChangeNumBigger() throws HttpFetcherException {
        when(mSplitsStorage.getTill()).thenReturn(mChangeNumber + 100L);

        mTask.execute();

        verify(mSplitsSyncHelper, never()).sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES));
    }

    @Test
    public void storedRbsChangeNumBigger() throws HttpFetcherException {
        when(mRuleBasedSegmentStorage.getChangeNumber()).thenReturn(mRbsChangeNumber + 100L);

        mTask.execute();

        verify(mSplitsSyncHelper, never()).sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES));
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
