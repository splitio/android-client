package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.helpers.FileHelper;

public class SplitSyncTaskTest {

    private static final long OLD_TIMESTAMP = 1546300800L; //2019-01-01

    SplitsStorage mSplitsStorage;
    SplitChange mSplitChange = null;
    SplitsSyncHelper mSplitsSyncHelper;

    SplitsSyncTask mTask;
    Map<String, Object> mDefaultParams = new HashMap<>();
    String mQueryString = "qs=1";

    SplitEventsManager mEventsManager;

    TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    @Before
    public void setup() {
        mTelemetryRuntimeProducer = mock(TelemetryRuntimeProducer.class);

        mDefaultParams.clear();
        mDefaultParams.put("since", -1L);
        mSplitsStorage = mock(SplitsStorage.class);
        mSplitsSyncHelper = mock(SplitsSyncHelper.class);
        mEventsManager = mock(SplitEventsManager.class);

        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean())).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLIT_KILL));

        loadSplitChanges();
    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        // Check that syncing is done with changeNum retrieved from db
        // Querystring is the same, so no clear sould be called
        // And updateTimestamp is 0
        // Retry is off, so splitSyncHelper.sync should be called
        mTask = new SplitsSyncTask(mSplitsSyncHelper, mSplitsStorage,
                false, 1000, mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);

        mTask.execute();

        verify(mSplitsSyncHelper, times(1)).sync(mDefaultParams, false, false);
    }

    @Test
    public void cleanOldCacheDisabled() throws HttpFetcherException {
    // Cache should not be cleared when cache expired
        mTask = new SplitsSyncTask(mSplitsSyncHelper, mSplitsStorage,
                false, 100L, mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(300L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(100L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        // This value function checks that cache is expired, here we simulate cache expired
        when(mSplitsSyncHelper.cacheHasExpired(anyLong(), anyLong(), anyLong())).thenReturn(true);

        mTask.execute();

        verify(mSplitsStorage, never()).clear();
    }

    @Test
    public void cleanOldCacheEnabled() throws HttpFetcherException {


        Map<String, Object> params = new HashMap<>();
        params.put("since", 100L);
        // Cache should be cleared when cache expired
        mTask = new SplitsSyncTask(mSplitsSyncHelper, mSplitsStorage,
                true, 100L, mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(100L); // Dummy value clearing depends on cacheHasExpired function value
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.cacheHasExpired(anyLong(), anyLong(), anyLong())).thenReturn(true);

        mTask.execute();

        verify(mSplitsSyncHelper, times(1)).sync(params, true, false);
    }

    @Test
    public void cleanSplitsWhenQueryStringHasChanged() throws HttpFetcherException {
        // Splits have to be cleared when query string on db is != than current one on current sdk client instance
        // Setting up cache not expired
        // splits change param should be -1

        String otherQs = "q=other";
        Map<String, Object> params = new HashMap<>();
        params.put("since", 100L);
        mTask = new SplitsSyncTask(mSplitsSyncHelper, mSplitsStorage,
                true, 100L, otherQs, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(1111L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.cacheHasExpired(anyLong(), anyLong(), anyLong())).thenReturn(false);

        mTask.execute();

        Map<String, Object> expectedParam = new HashMap<>();
        expectedParam.put("since", -1L);
        verify(mSplitsSyncHelper, times(1)).sync(expectedParam, true, false);
        verify(mSplitsStorage, times(1)).updateSplitsFilterQueryString(otherQs);
    }

    @Test
    public void noClearSplitsWhenQueryStringHasNotChanged() throws HttpFetcherException {
        // Splits have to be cleared when query string on db is != than current one on current sdk client instance
        // Setting up cache not expired

        Map<String, Object> params = new HashMap<>();
        params.put("since", 100L);
        mTask = new SplitsSyncTask(mSplitsSyncHelper, mSplitsStorage,
                true,100L, mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(1111L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.cacheHasExpired(anyLong(), anyLong(), anyLong())).thenReturn(false);

        mTask.execute();

        verify(mSplitsSyncHelper, times(1)).sync(params, false, false);
        verify(mSplitsStorage, never()).updateSplitsFilterQueryString(anyString());
    }

    @Test
    public void splitUpdatedNotified() throws HttpFetcherException {
        // Check that syncing is done with changeNum retrieved from db
        // Querystring is the same, so no clear sould be called
        // And updateTimestamp is 0
        // Retry is off, so splitSyncHelper.sync should be called
        mTask = new SplitsSyncTask(mSplitsSyncHelper, mSplitsStorage,
                false, 1000, mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(-1L).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean())).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mEventsManager, times(1)).notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
    }

    @Test
    public void splitFetchdNotified() throws HttpFetcherException {
        // Check that syncing is done with changeNum retrieved from db
        // Querystring is the same, so no clear sould be called
        // And updateTimestamp is 0
        // Retry is off, so splitSyncHelper.sync should be called
        mTask = new SplitsSyncTask(mSplitsSyncHelper, mSplitsStorage,
                false, 1000, mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean())).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mEventsManager, times(1)).notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED);
    }

    @Test
    public void syncIsTrackedInTelemetry() {
        mTask = new SplitsSyncTask(mSplitsSyncHelper, mSplitsStorage,
                false, 1000, mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean())).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mTelemetryRuntimeProducer).recordSyncLatency(eq(OperationType.SPLITS), anyLong());
    }

    @Test
    public void recordSuccessInTelemetry() {
        mTask = new SplitsSyncTask(mSplitsSyncHelper, mSplitsStorage,
                false, 1000, mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);

        mTask.execute();

        verify(mTelemetryRuntimeProducer).recordSuccessfulSync(eq(OperationType.SPLITS), longThat(arg -> arg > 0));
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
