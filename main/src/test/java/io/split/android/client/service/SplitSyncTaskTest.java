package io.split.android.client.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.api.EventMetadata;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageProducer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;
import io.split.android.helpers.FileHelper;

public class SplitSyncTaskTest {

    private static final long OLD_TIMESTAMP = 1546300800L; //2019-01-01

    SplitsStorage mSplitsStorage;
    SplitChange mSplitChange = null;
    SplitsSyncHelper mSplitsSyncHelper;
    RuleBasedSegmentStorageProducer mRuleBasedSegmentStorage;

    SplitsSyncTask mTask;
    String mQueryString = "qs=1";

    SplitEventsManager mEventsManager;

    TelemetryRuntimeProducer mTelemetryRuntimeProducer;

    @Before
    public void setup() {
        mTelemetryRuntimeProducer = mock(TelemetryRuntimeProducer.class);

        mSplitsStorage = mock(SplitsStorage.class);
        mSplitsSyncHelper = mock(SplitsSyncHelper.class);
        mEventsManager = mock(SplitEventsManager.class);
        mRuleBasedSegmentStorage = mock(RuleBasedSegmentStorageProducer.class);

        when(mSplitsSyncHelper.sync(notNull(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLIT_KILL));

        loadSplitChanges();
    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        // Check that syncing is done with changeNum retrieved from db
        // Querystring is the same, so no clear sould be called
        // And updateTimestamp is 0
        // Retry is off, so splitSyncHelper.sync should be called
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mRuleBasedSegmentStorage.getChangeNumber()).thenReturn(-1L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);

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
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                otherQs, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(100L);
        when(mRuleBasedSegmentStorage.getChangeNumber()).thenReturn(200L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(1111L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);

        mTask.execute();

        verify(mSplitsSyncHelper, times(1)).sync(argThat(argument -> argument.getFlagsSince() == -1 && argument.getRbsSince() == 200), eq(true), eq(true), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES));
        verify(mSplitsStorage, times(1)).updateSplitsFilterQueryString(otherQs);
    }

    @Test
    public void noClearSplitsWhenQueryStringHasNotChanged() throws HttpFetcherException {
        // Splits have to be cleared when query string on db is != than current one on current sdk client instance
        // Setting up cache not expired

        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(1111L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);

        mTask.execute();

        verify(mSplitsSyncHelper, times(1)).sync(argThat(argument -> argument.getFlagsSince() == 100L), eq(false), eq(false), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES));
        verify(mSplitsStorage, never()).updateSplitsFilterQueryString(anyString());
    }

    @Test
    public void splitUpdatedNotified() throws HttpFetcherException {
        // Check that both SPLITS_SYNC_COMPLETE and SPLITS_UPDATED are notified
        // when sync completes with data changes
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(-1L).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mEventsManager, times(1)).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
        verify(mEventsManager, times(1)).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), any());
    }

    @Test
    public void splitSyncCompleteNotifiedWhenNoDataChange() throws HttpFetcherException {
        // Check that SPLITS_SYNC_COMPLETE is notified when sync completes
        // but no data changes (SPLITS_UPDATED should NOT be notified)
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mEventsManager, times(1)).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
        verify(mEventsManager, never()).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), any());
    }

    @Test
    public void syncIsTrackedInTelemetry() {
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mTelemetryRuntimeProducer).recordSyncLatency(eq(OperationType.SPLITS), anyLong());
    }

    @Test
    public void recordSuccessInTelemetry() {
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(-1L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);

        mTask.execute();

        verify(mTelemetryRuntimeProducer).recordSuccessfulSync(eq(OperationType.SPLITS), longThat(arg -> arg > 0));
    }

    @Test
    public void targetingRulesSyncCompleteIsAlwaysFiredOnSuccessfulSync() throws HttpFetcherException {
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mEventsManager).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
    }

    @Test
    public void splitsUpdatedIsFiredWhenDataChanged() throws HttpFetcherException {
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);

        when(mSplitsStorage.getTill()).thenReturn(-1L).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mEventsManager).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), any());
    }

    @Test
    public void splitsUpdatedIsNotFiredWhenDataUnchanged() throws HttpFetcherException {
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);

        when(mSplitsStorage.getTill()).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        mTask.execute();

        verify(mEventsManager, never()).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), any());
        verify(mEventsManager).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE));
    }

    @Test
    public void splitsUpdatedIncludesMetadataWithUpdatedFlags() throws HttpFetcherException {
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(-1L).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        // Mock the updated split names
        List<String> updatedSplitNames = Arrays.asList("split1", "split2", "split3");
        when(mSplitsSyncHelper.getLastUpdatedSplitNames()).thenReturn(updatedSplitNames);

        mTask.execute();

        verify(mEventsManager).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), argThat(metadata -> {
            if (metadata == null) return false;
            assertTrue(metadata.containsKey("updatedFlags"));
            Object flagsValue = metadata.get("updatedFlags");
            assertNotNull(flagsValue);
            assertTrue(flagsValue instanceof List);
            @SuppressWarnings("unchecked")
            List<String> flags = (List<String>) flagsValue;
            assertEquals(3, flags.size());
            assertTrue(flags.contains("split1"));
            assertTrue(flags.contains("split2"));
            assertTrue(flags.contains("split3"));
            return true;
        }));
    }

    @Test
    public void splitsUpdatedIncludesEmptyMetadataWhenNoSplitsUpdated() throws HttpFetcherException {
        mTask = SplitsSyncTask.build(mSplitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorage,
                mQueryString, mEventsManager, mTelemetryRuntimeProducer);
        when(mSplitsStorage.getTill()).thenReturn(-1L).thenReturn(100L);
        when(mSplitsStorage.getUpdateTimestamp()).thenReturn(0L);
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn(mQueryString);
        when(mSplitsSyncHelper.sync(any(), anyBoolean(), anyBoolean(), eq(ServiceConstants.ON_DEMAND_FETCH_BACKOFF_MAX_RETRIES))).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC));

        // Mock empty updated split names
        when(mSplitsSyncHelper.getLastUpdatedSplitNames()).thenReturn(new ArrayList<>());

        mTask.execute();

        verify(mEventsManager).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), argThat(metadata -> {
            if (metadata == null) return false;
            assertTrue(metadata.containsKey("updatedFlags"));
            Object flagsValue = metadata.get("updatedFlags");
            assertNotNull(flagsValue);
            assertTrue(flagsValue instanceof List);
            @SuppressWarnings("unchecked")
            List<String> flags = (List<String>) flagsValue;
            assertTrue(flags.isEmpty());
            return true;
        }));
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
