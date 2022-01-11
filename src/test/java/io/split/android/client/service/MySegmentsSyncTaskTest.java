package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.mysegments.MySegmentsSyncTask;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

public class MySegmentsSyncTaskTest {

    Map<String, Object> noParams = Collections.emptyMap();
    Map<String, String> noHeaders = Collections.emptyMap();

    @Mock
    HttpFetcher mMySegmentsFetcher;
    @Mock
    MySegmentsStorage mySegmentsStorage;
    @Mock
    SplitEventsManager mEventsManager;

    List<MySegment> mMySegments = null;

    MySegmentsSyncTask mTask;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, false, mEventsManager);
        loadMySegments();
    }

    @Test
    public void correctExecution() throws HttpFetcherException {


        when(mMySegmentsFetcher.execute(noParams, null)).thenReturn(mMySegments);

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
        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, true, null);
        when(mMySegmentsFetcher.execute(noParams, headers)).thenReturn(mMySegments);

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
        mTask = new MySegmentsSyncTask(mMySegmentsFetcher, mySegmentsStorage, false, mEventsManager);
        when(mMySegmentsFetcher.execute(noParams, null)).thenThrow(IllegalStateException.class);

        mTask.execute();

        verify(mMySegmentsFetcher, times(1)).execute(noParams, null);
        verify(mySegmentsStorage, never()).set(any());
    }

    @Test
    public void storageException() throws HttpFetcherException {
        when(mMySegmentsFetcher.execute(noParams, null)).thenReturn(mMySegments);
        doThrow(NullPointerException.class).when(mySegmentsStorage).set(any());

        mTask.execute();

        verify(mMySegmentsFetcher, times(1)).execute(noParams, null);
        verify(mySegmentsStorage, times(1)).set(any());
    }

    @Test
    public void addHttpStatusWhenHttpRequestFails() throws HttpFetcherException {
        when(mMySegmentsFetcher.execute(noParams, null)).thenThrow(new HttpFetcherException("", "", 500));

        SplitTaskExecutionInfo result = mTask.execute();

        Assert.assertEquals(500, result.getIntegerValue("HTTP_STATUS").intValue());
    }

    @After
    public void tearDown() {
        reset(mMySegmentsFetcher);
        reset(mySegmentsStorage);
    }

    private void loadMySegments() {
        if (mMySegments == null) {
            mMySegments = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                MySegment s = new MySegment();
                s.id = "id_" + i;
                s.id = "segment_" + i;
                mMySegments.add(s);
            }
        }
    }
}
