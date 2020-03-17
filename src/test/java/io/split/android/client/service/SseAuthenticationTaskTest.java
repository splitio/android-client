package io.split.android.client.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.sseauthentication.SseAuthenticationTask;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.service.sseclient.SseChannelsParser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SseAuthenticationTaskTest {

    HttpFetcher<SseAuthenticationResponse> mFetcher;
    SseAuthenticationResponse mAuthResponse;

    SseAuthenticationTask mTask;

    SseChannelsParser mChannelParser;

    private final String JWT = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleUlkIiwidHlwIjoiSldUIn0.eyJvcmdJ" +
            "ZCI6ImY3ZjAzNTIwLTVkZjctMTFlOC04NDc2LTBlYzU0NzFhM2NlYyIsImVudklkIjoiZjdmN" +
            "jI4OTAtNWRmNy0xMWU4LTg0NzYtMGVjNTQ3MWEzY2VjIiwidXNlcktleXMiOlsiamF2aSJdLC" +
            "J4LWFibHktY2FwYWJpbGl0eSI6IntcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X01" +
            "UY3dOVEkyTVRNME1nPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk16TTVOamMwT" +
            "0RjeU5nPT1fTVRFeE16Z3dOamd4X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2x" +
            "cIjpbXCJzdWJzY3JpYmVcIl19IiwieC1hYmx5LWNsaWVudElkIjoiY2xpZW50SWQiLCJleHAiOj" +
            "E1ODM5NDc4MTIsImlhdCI6MTU4Mzk0NDIxMn0.bSkxugrXKLaJJkvlND1QEd7vrwqWiPjn77pkrJOl4t8";

    @Before
    public void setup() {
        mChannelParser = Mockito.mock(SseChannelsParser.class);
        mFetcher = (HttpFetcher<SseAuthenticationResponse>) Mockito.mock(HttpFetcher.class);
        mAuthResponse = Mockito.mock(SseAuthenticationResponse.class);
        mTask = new SseAuthenticationTask(mFetcher, "api", "userKey", mChannelParser);
    }

    @Test
    public void correctExecutionOk() throws HttpFetcherException {

        List<String> mockChannelList = new ArrayList<>();
        mockChannelList.add("channel1");
        mockChannelList.add("channel2");
        mockChannelList.add("channel3");

        when(mAuthResponse.isValidApiKey()).thenReturn(true);
        when(mAuthResponse.isStreamingEnabled()).thenReturn(true);
        when(mAuthResponse.getToken()).thenReturn(JWT);

        when(mFetcher.execute(any())).thenReturn(mAuthResponse);

        when(mChannelParser.parse(any())).thenReturn(mockChannelList);

        SplitTaskExecutionInfo info = mTask.execute();

        verify(mFetcher, times(1)).execute(any());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, info.getStatus());

        List<String> channelList = (List<String>) info.getObjectValue(SplitTaskExecutionInfo.CHANNEL_LIST_PARAM);
        Assert.assertEquals(3, channelList.size());
        Assert.assertEquals("channel1", channelList.get(0));
        Assert.assertEquals(true, info.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY));
        Assert.assertEquals(true, info.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED));
        Assert.assertEquals(JWT, info.getStringValue(SplitTaskExecutionInfo.SSE_TOKEN));
    }

    @Test
    public void correctExecutionInvalidApiAndKey() throws HttpFetcherException {

        List<String> mockList = new ArrayList<>();

        when(mAuthResponse.isValidApiKey()).thenReturn(false);
        when(mAuthResponse.isStreamingEnabled()).thenReturn(false);
        when(mAuthResponse.getToken()).thenReturn(null);

        when(mFetcher.execute(any())).thenReturn(mAuthResponse);

        when(mChannelParser.parse(any())).thenReturn(mockList);


        SplitTaskExecutionInfo info = mTask.execute();

        List<String> channelList = (List<String>) info.getObjectValue(SplitTaskExecutionInfo.CHANNEL_LIST_PARAM);

        verify(mFetcher, times(1)).execute(any());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, info.getStatus());
        Assert.assertEquals(0, channelList.size());
        Assert.assertEquals(false, info.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY));
        Assert.assertEquals(false, info.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED));
        Assert.assertNull(info.getStringValue(SplitTaskExecutionInfo.SSE_TOKEN));
    }

    @Test
    public void fetcherException() throws HttpFetcherException {
        doThrow(NullPointerException.class).when(mFetcher).execute(any());

        SplitTaskExecutionInfo info = mTask.execute();
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, info.getStatus());
        Assert.assertNull(info.getStringValue(SplitTaskExecutionInfo.CHANNEL_LIST_PARAM));
        Assert.assertNull(info.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY));
        Assert.assertNull(info.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED));
        Assert.assertNull(info.getStringValue(SplitTaskExecutionInfo.SSE_TOKEN));
    }

    @After
    public void tearDown() {
        reset(mFetcher);
    }
}
