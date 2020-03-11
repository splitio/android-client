package io.split.android.client.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.splits.SseAuthenticationTask;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.helpers.FileHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SseAuthenticationTaskTest {

    HttpFetcher<SseAuthenticationResponse> mFetcher;
    SseAuthenticationResponse mAuthResponse = null;

    SseAuthenticationTask mTask;

    @Before
    public void setup() {
        mFetcher = (HttpFetcher<SseAuthenticationResponse>) Mockito.mock(HttpFetcher.class);
        mTask = new SseAuthenticationTask(mFetcher, "api", "userKey");
    }

    @Test
    public void correctExecutionOk() throws HttpFetcherException {
        mAuthResponse = new SseAuthenticationResponse(true, true, "oneToken");

        when(mFetcher.execute(any())).thenReturn(mAuthResponse);

        SplitTaskExecutionInfo info = mTask.execute();

        verify(mFetcher, times(1)).execute(any());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, info.getStatus());
        Assert.assertEquals("oneToken", info.getStringValue(SplitTaskExecutionInfo.SSE_AUTH_TOKEN));
        Assert.assertEquals(true, info.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY));
        Assert.assertEquals(true, info.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED));
    }

    @Test
    public void correctExecutionInvalidApiAndKey() throws HttpFetcherException {
        mAuthResponse = new SseAuthenticationResponse(false, false, "otherToken");

        when(mFetcher.execute(any())).thenReturn(mAuthResponse);

        SplitTaskExecutionInfo info = mTask.execute();

        verify(mFetcher, times(1)).execute(any());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, info.getStatus());
        Assert.assertEquals("otherToken", info.getStringValue(SplitTaskExecutionInfo.SSE_AUTH_TOKEN));
        Assert.assertEquals(false, info.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY));
        Assert.assertEquals(false, info.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED));
    }

    // TODO: When response info updated. Error execution test
    @Test
    public void errorExecution() throws HttpFetcherException {
//        mAuthResponse = new SseAuthenticationResponse(false, false, "otherToken");
//
//        when(mFetcher.execute(any())).thenReturn(mAuthResponse);
//
//        SplitTaskExecutionInfo info = mTask.execute();
//
//        verify(mFetcher, times(1)).execute(any());
//        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, info.getStatus());
//        Assert.assertNull(info.getStringValue(SplitTaskExecutionInfo.SSE_AUTH_TOKEN));
//        Assert.assertNull(info.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY));
//        Assert.assertNull(info.getStringValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED));
    }

    @Test
    public void fetcherException() throws HttpFetcherException {
        mAuthResponse = new SseAuthenticationResponse(false, false, "otherToken");

        doThrow(NullPointerException.class).when(mFetcher).execute(any());

        SplitTaskExecutionInfo info = mTask.execute();
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, info.getStatus());
        Assert.assertNull(info.getStringValue(SplitTaskExecutionInfo.SSE_AUTH_TOKEN));
        Assert.assertNull(info.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY));
        Assert.assertNull(info.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED));
    }

    @After
    public void tearDown() {
        reset(mFetcher);
    }
}
