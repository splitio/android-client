package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.dtos.SegmentsChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTaskConfig;
import io.split.android.client.service.sseclient.BackoffCounter;
import io.split.android.client.service.synchronizer.MySegmentsChangeChecker;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.storage.TelemetryRuntimeProducer;

public class MySegmentsSyncTaskTest {

    Map<String, Object> noParams = Collections.emptyMap();

    @Mock
    HttpFetcher<AllSegmentsChange> mMySegmentsFetcher;
    @Mock
    MySegmentsStorage mySegmentsStorage;
    @Mock
    MySegmentsStorage myLargeSegmentsStorage;
    @Mock
    SplitEventsManager mEventsManager;
    @Mock
    TelemetryRuntimeProducer mTelemetryRuntimeProducer;
    @Mock
    MySegmentsChangeChecker mMySegmentsChangeChecker;

    AllSegmentsChange mMySegments = null;

    MySegmentsSyncTask mTask;
    private AutoCloseable mAutoCloseable;

    @Before
    public void setup() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);
        when(mMySegmentsChangeChecker.mySegmentsHaveChanged(any(), any())).thenReturn(true);
        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), null, null);
        loadMySegments();
    }

    @After
    public void tearDown() {
        try {
            mAutoCloseable.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void correctExecution() throws HttpFetcherException {
        when(mMySegmentsFetcher.execute(noParams, null)).thenAnswer((Answer<AllSegmentsChange>) invocation -> mMySegments);

        mTask.execute();

        ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mMySegmentsFetcher).execute(any(), headersCaptor.capture());
        verify(mMySegmentsFetcher, times(1)).execute(noParams, null);
        verify(mySegmentsStorage, times(1)).set(any());

        Assert.assertNull(headersCaptor.getValue());
    }

    @Test
    public void correctExecutionNoCache() throws HttpFetcherException {
        Map<String, String> headers = new HashMap<>();
        headers.put(SplitHttpHeadersBuilder.CACHE_CONTROL_HEADER, SplitHttpHeadersBuilder.CACHE_CONTROL_NO_CACHE);
        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, true, null, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), null, null);
        when(mMySegmentsFetcher.execute(noParams, headers)).thenAnswer((Answer<AllSegmentsChange>) invocation -> mMySegments);

        mTask.execute();

        ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mMySegmentsFetcher, times(1)).execute(any(), headersCaptor.capture());
        verify(mySegmentsStorage, times(1)).set(any());
        Assert.assertEquals(SplitHttpHeadersBuilder.CACHE_CONTROL_NO_CACHE, headersCaptor.getValue().get(SplitHttpHeadersBuilder.CACHE_CONTROL_HEADER));
    }

    @Test
    public void fetcherExceptionRetryOff() throws HttpFetcherException {
        when(mMySegmentsFetcher.execute(noParams, null)).thenThrow(HttpFetcherException.class);

        mTask.execute();

        verify(mMySegmentsFetcher, times(1)).execute(noParams, null);
        verify(mySegmentsStorage, never()).set(any());
    }

    @Test
    public void fetcherOtherExceptionRetryOn() throws HttpFetcherException {
        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), null, null);
        when(mMySegmentsFetcher.execute(noParams, null)).thenThrow(IllegalStateException.class);

        mTask.execute();

        verify(mMySegmentsFetcher, times(1)).execute(noParams, null);
        verify(mySegmentsStorage, never()).set(any());
    }

    @Test
    public void storageException() throws HttpFetcherException {
        when(mMySegmentsFetcher.execute(noParams, null)).thenAnswer((Answer<AllSegmentsChange>) invocation -> mMySegments);
        doThrow(NullPointerException.class).when(mySegmentsStorage).set(any());

        mTask.execute();

        verify(mMySegmentsFetcher, times(1)).execute(noParams, null);
        verify(mySegmentsStorage, times(1)).set(any());
    }

    @Test
    public void errorIsTrackedInTelemetry() throws HttpFetcherException {
        when(mMySegmentsFetcher.execute(noParams, null)).thenThrow(new HttpFetcherException("", "", 500));

        mTask.execute();

        verify(mTelemetryRuntimeProducer).recordSyncError(OperationType.MY_SEGMENT, 500);
    }

    @Test
    public void latencyIsTrackedInTelemetry() throws HttpFetcherException {
        when(mMySegmentsFetcher.execute(noParams, null)).thenAnswer((Answer<AllSegmentsChange>) invocation -> mMySegments);

        mTask.execute();

        verify(mTelemetryRuntimeProducer).recordSyncLatency(eq(OperationType.MY_SEGMENT), anyLong());
    }

    @Test
    public void successIsTrackedInTelemetry() throws HttpFetcherException {
        when(mMySegmentsFetcher.execute(noParams, null)).thenAnswer((Answer<AllSegmentsChange>) invocation -> mMySegments);

        mTask.execute();

        verify(mTelemetryRuntimeProducer).recordSuccessfulSync(eq(OperationType.MY_SEGMENT), longThat(arg -> arg > 0));
    }

    @Test
    public void statusCode9009InFetcherReturnsDoNotRetry() throws HttpFetcherException {
        when(mMySegmentsFetcher.execute(noParams, null)).thenThrow(new HttpFetcherException("", "", 9009));

        SplitTaskExecutionInfo result = mTask.execute();

        Assert.assertEquals(Boolean.TRUE, result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void nullStatusCodeReturnsNullDoNotRetry() throws HttpFetcherException {
        when(mMySegmentsFetcher.execute(noParams, null)).thenThrow(new HttpFetcherException("", "", null));

        SplitTaskExecutionInfo result = mTask.execute();

        Assert.assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, result.getStatus());
    }

    @Test
    public void successfulCallToExecuteReturnsNullDoNotRetry() throws HttpFetcherException {
        when(mMySegmentsFetcher.execute(any(), any())).thenAnswer((Answer<AllSegmentsChange>) invocation -> mMySegments);

        SplitTaskExecutionInfo result = mTask.execute();

        Assert.assertNull(result.getBoolValue(SplitTaskExecutionInfo.DO_NOT_RETRY));
        Assert.assertEquals(result.getStatus(), SplitTaskExecutionStatus.SUCCESS);
    }

    @Test
    public void addTillParameterToRequestWhenResponseCnDoesNotMatchTargetAndRetryLimitIsReached() throws HttpFetcherException {
        long targetLargeSegmentsChangeNumber = 4L;
        BackoffCounter backoffCounter = mock(BackoffCounter.class);
        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), null, targetLargeSegmentsChangeNumber,
                backoffCounter, 2);
        when(mMySegmentsFetcher.execute(noParams, null))
                .thenReturn(createChange(1L));
        when(mMySegmentsFetcher.execute(Collections.singletonMap("till", 4L), null))
                .thenReturn(createChange(4L));

        SplitTaskExecutionInfo result = mTask.execute();

        verify(backoffCounter).resetCounter();
        verify(mMySegmentsFetcher, times(2)).execute(noParams, null);
        verify(mMySegmentsFetcher, times(1)).execute(Collections.singletonMap("till", 4L), null);
        verify(backoffCounter, times(2)).getNextRetryTime();
    }

    @Test
    public void fetchedEventIsEmittedWhenNoChangesInSegments() throws HttpFetcherException {
        when(mMySegmentsChangeChecker.mySegmentsHaveChanged(any(), any())).thenReturn(false);
        when(mMySegmentsFetcher.execute(noParams, null)).thenReturn(mMySegments);

        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), null, null, mock(BackoffCounter.class), 1);
        mTask.execute();

        verify(mEventsManager).notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_FETCHED);
    }

    @Test
    public void updatedEventIsEmittedWhenChangesInSegments() throws HttpFetcherException {
        when(mMySegmentsChangeChecker.mySegmentsHaveChanged(any(), any())).thenReturn(true);
        when(mMySegmentsFetcher.execute(noParams, null)).thenReturn(mMySegments);

        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), null, null, mock(BackoffCounter.class), 1);
        mTask.execute();

        verify(mEventsManager).notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);
    }

    @Test
    public void largeSegmentsUpdatedEventIsEmittedWhenChangesInLargeSegmentsAndNotInSegments() throws HttpFetcherException {
        when(mMySegmentsChangeChecker.mySegmentsHaveChanged(any(), any())).thenReturn(false);
        when(mMySegmentsChangeChecker.mySegmentsHaveChanged(Collections.emptyList(), Collections.singletonList("largesegment0"))).thenReturn(true);
        when(mMySegmentsFetcher.execute(noParams, null)).thenReturn(createChange(1L));

        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), null, null, mock(BackoffCounter.class), 1);
        mTask.execute();

        verify(mEventsManager).notifyInternalEvent(SplitInternalEvent.MY_LARGE_SEGMENTS_UPDATED);
    }

    @Test
    public void largeSegmentsTargetIsUsedForCdnBypassWhenSegmentsChangeNumberIsNotSet() throws HttpFetcherException {
        long targetLargeSegmentsChangeNumber = 4L;
        when(mMySegmentsFetcher.execute(noParams, null))
                .thenReturn(createChange(null, 1L));
        when(mMySegmentsFetcher.execute(Collections.singletonMap("till", 4L), null))
                .thenReturn(createChange(null, 4L));

        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), null, targetLargeSegmentsChangeNumber, mock(BackoffCounter.class), 1);
        mTask.execute();

        verify(mMySegmentsFetcher, times(2)).execute(any(), any());
        verify(mMySegmentsFetcher).execute(noParams, null);
        verify(mMySegmentsFetcher).execute(Collections.singletonMap("till", 4L), null);
    }

    @Test
    public void segmentsTargetIsUsedForCdnBypassWhenLargeSegmentsChangeNumberIsNotSet() throws HttpFetcherException {
        long targetSegmentsChangeNumber = 5L;
        when(mMySegmentsFetcher.execute(noParams, null))
                .thenReturn(createChange(2L, null));
        when(mMySegmentsFetcher.execute(Collections.singletonMap("till", 5L), null))
                .thenReturn(createChange(5L, null));

        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), targetSegmentsChangeNumber, null, mock(BackoffCounter.class), 1);
        mTask.execute();

        verify(mMySegmentsFetcher, times(2)).execute(any(), any());
        verify(mMySegmentsFetcher).execute(noParams, null);
        verify(mMySegmentsFetcher).execute(Collections.singletonMap("till", 5L), null);
    }

    @Test
    public void largeSegmentsTargetIsUsedForCdnBypassWhenSegmentsChangeNumberIsSmaller() throws HttpFetcherException {
        long targetLargeSegmentsChangeNumber = 4L;
        when(mMySegmentsFetcher.execute(noParams, null))
                .thenReturn(createChange(2L, 1L));
        when(mMySegmentsFetcher.execute(Collections.singletonMap("till", 4L), null))
                .thenReturn(createChange(2L, 4L));

        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), null, targetLargeSegmentsChangeNumber, mock(BackoffCounter.class), 1);
        mTask.execute();

        verify(mMySegmentsFetcher, times(2)).execute(any(), any());
        verify(mMySegmentsFetcher).execute(noParams, null);
        verify(mMySegmentsFetcher).execute(Collections.singletonMap("till", 4L), null);
    }

    @Test
    public void segmentsTargetIsUsedForCdnBypassWhenLargeSegmentsChangeNumberIsSmaller() throws HttpFetcherException {
        long targetSegmentsChangeNumber = 5L;
        when(mMySegmentsFetcher.execute(noParams, null))
                .thenReturn(createChange(2L, 1L));
        when(mMySegmentsFetcher.execute(Collections.singletonMap("till", 5L), null))
                .thenReturn(createChange(5L, 1L));

        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), targetSegmentsChangeNumber, null, mock(BackoffCounter.class), 1);
        mTask.execute();

        verify(mMySegmentsFetcher, times(2)).execute(any(), any());
        verify(mMySegmentsFetcher).execute(noParams, null);
        verify(mMySegmentsFetcher).execute(Collections.singletonMap("till", 5L), null);
    }

    @Test
    public void noFetchWhenSegmentsChangeNumberInStorageIsNewerThanTarget() throws HttpFetcherException {
        long targetSegmentsChangeNumber = 5L;
        when(mySegmentsStorage.getChangeNumber()).thenReturn(6L);
        when(myLargeSegmentsStorage.getChangeNumber()).thenReturn(5L);

        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), targetSegmentsChangeNumber, null, mock(BackoffCounter.class), 1);
        mTask.execute();

        verify(mMySegmentsFetcher, never()).execute(any(), any());
    }

    @Test
    public void noFetchWhenLargeSegmentsChangeNumberIsNewerThanTarget() throws HttpFetcherException {
        long targetLargeSegmentsChangeNumber = 4L;
        when(mySegmentsStorage.getChangeNumber()).thenReturn(3L);
        when(myLargeSegmentsStorage.getChangeNumber()).thenReturn(5L);

        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), null, targetLargeSegmentsChangeNumber, mock(BackoffCounter.class), 1);
        mTask.execute();

        verify(mMySegmentsFetcher, never()).execute(any(), any());
    }

    @Test
    public void fetchWhenSegmentsChangeNumberInStorageIsNewerThanTargetAndLargeSegmentsChangeNumberIsOlder() throws HttpFetcherException {
        long targetSegmentsChangeNumber = 5L;
        long targetLargeSegmentsChangeNumber = 4L;
        when(mySegmentsStorage.getChangeNumber()).thenReturn(6L);
        when(myLargeSegmentsStorage.getChangeNumber()).thenReturn(3L);
        when(mMySegmentsFetcher.execute(noParams, null))
                .thenReturn(createChange(6L, 4L));

        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), targetSegmentsChangeNumber, targetLargeSegmentsChangeNumber, mock(BackoffCounter.class), 1);
        mTask.execute();

        verify(mMySegmentsFetcher).execute(any(), any());
        verify(mMySegmentsFetcher).execute(noParams, null);
    }

    @Test
    public void fetchIsPerformedWhenLargeSegmentsChangeNumberInStorageIsNewerThanTargetAndSegmentsChangeNumberIsOlder() throws HttpFetcherException {
        long targetSegmentsChangeNumber = 5L;
        long targetLargeSegmentsChangeNumber = 4L;
        when(mySegmentsStorage.getChangeNumber()).thenReturn(3L);
        when(myLargeSegmentsStorage.getChangeNumber()).thenReturn(6L);
        when(mMySegmentsFetcher.execute(noParams, null))
                .thenReturn(createChange(5L, 6L));

        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mMySegmentsChangeChecker, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get(), targetSegmentsChangeNumber, targetLargeSegmentsChangeNumber, mock(BackoffCounter.class), 1);
        mTask.execute();

        verify(mMySegmentsFetcher).execute(any(), any());
        verify(mMySegmentsFetcher).execute(noParams, null);
    }

    @NonNull
    private static AllSegmentsChange createChange(Long msChangeNumber, Long lsChangeNumber) {
        return AllSegmentsChange.create(
                SegmentsChange.create(new HashSet<>(Collections.singletonList("segment0")), msChangeNumber),
                SegmentsChange.create(new HashSet<>(Collections.singletonList("largesegment0")), lsChangeNumber));
    }

    @NonNull
    private static AllSegmentsChange createChange(long lsChangeNumber) {
        return createChange(null, lsChangeNumber);
    }

    private void loadMySegments() {
        if (mMySegments == null) {
            Set<String> segments = new HashSet<>();
            for (int i = 0; i < 5; i++) {
                segments.add("segment_" + i);
            }
            mMySegments = AllSegmentsChange.create(SegmentsChange.create(segments, null), SegmentsChange.createEmpty());
        }
    }
}
