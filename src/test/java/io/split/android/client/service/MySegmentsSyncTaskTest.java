package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.service.mysegments.MySegmentsSyncTaskConfig;
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

    AllSegmentsChange mMySegments = null;

    MySegmentsSyncTask mTask;
    private AutoCloseable mAutoCloseable;

    @Before
    public void setup() {
        mAutoCloseable = MockitoAnnotations.openMocks(this);
        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get());
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
        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, true, null, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get());
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
        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, myLargeSegmentsStorage, false, mEventsManager, mTelemetryRuntimeProducer, MySegmentsSyncTaskConfig.get());
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

    private void loadMySegments() {
        if (mMySegments == null) {
            List<MySegment> segments = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                MySegment s = new MySegment();
                s.id = "id_" + i;
                s.name = "segment_" + i;
                segments.add(s);
            }
            // TODO legacy endpoint support
            mMySegments = new AllSegmentsChange(segments.stream().map(s -> s.name).collect(Collectors.toList()));
        }
    }
}
