package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

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
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.helpers.FileHelper;

public class SplitSyncTaskTest {

    private static final long OLD_TIMESTAMP = 1546300800L; //2019-01-01

    SplitChange mSplitChange = null;
    SplitsSyncHelper mSplitsSyncHelper;
    private GeneralInfoStorage mGeneralInfoStorage;

    SplitsSyncTask mTask;
    String mQueryString = "qs=1";

    SplitEventsManager mEventsManager;

    TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    @Before
    public void setup() {
        mTelemetryRuntimeProducer = mock(TelemetryRuntimeProducer.class);

        mSplitsSyncHelper = mock(SplitsSyncHelper.class);
        mEventsManager = mock(SplitEventsManager.class);
        mGeneralInfoStorage = mock(GeneralInfoStorage.class);

        when(mSplitsSyncHelper.sync(notNull(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLIT_KILL));

        loadSplitChanges();
    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        // Check that syncing is done with changeNum retrieved from db
        // Querystring is the same, so no clear sould be called
        // And updateTimestamp is 0
        // Retry is off, so splitSyncHelper.sync should be called
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mGeneralInfoStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mGeneralInfoStorage.getFlagsChangeNumber()).thenReturn(-1L);
        when(mGeneralInfoStorage.getRbsChangeNumber()).thenReturn(-1L);
        when(mGeneralInfoStorage.getSplitsUpdateTimestamp()).thenReturn(0L);
        when(mGeneralInfoStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);

        mTask.execute();

        verify(mSplitsSyncHelper, times(1)).sync(argThat(argument -> argument.getFlagsSince() == -1L && argument.getRbsSince() == -1L), eq(false), eq(false), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES));
    }

    @Test
    public void cleanSplitsWhenQueryStringHasChanged() throws HttpFetcherException {
        // Splits have to be cleared when query string on db is != than current one on current sdk client instance
        // Setting up cache not expired
        // splits change param should be -1

        String otherQs = "q=other";
        Map<String, Object> params = new HashMap<>();
        params.put("since", 100L);
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mGeneralInfoStorage,
                otherQs, mEventsManager, mTelemetryRuntimeProducer);
        when(mGeneralInfoStorage.getFlagsChangeNumber()).thenReturn(100L);
        when(mGeneralInfoStorage.getRbsChangeNumber()).thenReturn(200L);
        when(mGeneralInfoStorage.getSplitsUpdateTimestamp()).thenReturn(1111L);
        when(mGeneralInfoStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);

        mTask.execute();

        verify(mSplitsSyncHelper, times(1)).sync(argThat(argument -> argument.getFlagsSince() == -1 && argument.getRbsSince() == 200), eq(true), eq(true), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES));
        verify(mGeneralInfoStorage, times(1)).setSplitsFilterQueryString(otherQs);
    }

    @Test
    public void noClearSplitsWhenQueryStringHasNotChanged() throws HttpFetcherException {
        // Splits have to be cleared when query string on db is != than current one on current sdk client instance
        // Setting up cache not expired

        mTask = buildSplitsSyncTask();
        when(mGeneralInfoStorage.getFlagsChangeNumber()).thenReturn(100L);
        when(mGeneralInfoStorage.getSplitsUpdateTimestamp()).thenReturn(1111L);
        when(mGeneralInfoStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);

        mTask.execute();

        verify(mSplitsSyncHelper, times(1)).sync(argThat(argument -> argument.getFlagsSince() == 100L), eq(false), eq(false), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES));
        verify(mGeneralInfoStorage, never()).setSplitsFilterQueryString(anyString());
    }

    @Test
    public void splitUpdatedNotified() throws HttpFetcherException {
        // Check that syncing is done with changeNum retrieved from db
        // Querystring is the same, so no clear sould be called
        // And updateTimestamp is 0
        // Retry is off, so splitSyncHelper.sync should be called
        mTask = buildSplitsSyncTask();
        when(mGeneralInfoStorage.getFlagsChangeNumber()).thenReturn(-1L).thenReturn(100L);
        when(mGeneralInfoStorage.getSplitsUpdateTimestamp()).thenReturn(0L);
        when(mGeneralInfoStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mEventsManager, times(1)).notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
    }

    @Test
    public void splitFetchdNotified() throws HttpFetcherException {
        // Check that syncing is done with changeNum retrieved from db
        // Querystring is the same, so no clear sould be called
        // And updateTimestamp is 0
        // Retry is off, so splitSyncHelper.sync should be called
        mTask = buildSplitsSyncTask();
        when(mGeneralInfoStorage.getFlagsChangeNumber()).thenReturn(100L);
        when(mGeneralInfoStorage.getSplitsUpdateTimestamp()).thenReturn(0L);
        when(mGeneralInfoStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mEventsManager, times(1)).notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED);
    }

    @Test
    public void syncIsTrackedInTelemetry() {
        mTask = buildSplitsSyncTask();
        when(mGeneralInfoStorage.getFlagsChangeNumber()).thenReturn(100L);
        when(mGeneralInfoStorage.getSplitsUpdateTimestamp()).thenReturn(0L);
        when(mGeneralInfoStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mTelemetryRuntimeProducer).recordSyncLatency(eq(OperationType.SPLITS), anyLong());
    }

    @Test
    public void recordSuccessInTelemetry() {
        mTask = buildSplitsSyncTask();
        when(mGeneralInfoStorage.getFlagsChangeNumber()).thenReturn(-1L);
        when(mGeneralInfoStorage.getSplitsUpdateTimestamp()).thenReturn(0L);
        when(mGeneralInfoStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);

        mTask.execute();

        verify(mTelemetryRuntimeProducer).recordSuccessfulSync(eq(OperationType.SPLITS), longThat(arg -> arg > 0));
    }

    @NonNull
    private SplitsSyncTask buildSplitsSyncTask() {
        return SplitsSyncTask.build(mSplitsSyncHelper, mGeneralInfoStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);
    }

    @After
    public void tearDown() {
        reset(mGeneralInfoStorage);
    }

    private void loadSplitChanges() {
        if (mSplitChange == null) {
            FileHelper fileHelper = new FileHelper();
            mSplitChange = fileHelper.loadSplitChangeFromFile("split_changes_1.json");
        }
    }
}
