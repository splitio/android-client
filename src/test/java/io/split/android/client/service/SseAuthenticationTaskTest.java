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
import io.split.android.client.service.sseclient.InvalidJwtTokenException;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;
import io.split.android.client.service.sseclient.SseJwtParser;
import io.split.android.client.service.sseclient.SseJwtToken;

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

    SseJwtParser mJwtParser;

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
        mJwtParser = Mockito.mock(SseJwtParser.class);
        mFetcher = (HttpFetcher<SseAuthenticationResponse>) Mockito.mock(HttpFetcher.class);
        mAuthResponse = Mockito.mock(SseAuthenticationResponse.class);
        mTask = new SseAuthenticationTask(mFetcher, "userKey", mJwtParser);
    }

    @Test
    public void correctExecutionOk() throws HttpFetcherException, InvalidJwtTokenException {

        List<String> mockChannelList = new ArrayList<>();
        mockChannelList.add("channel1");
        mockChannelList.add("channel2");
        mockChannelList.add("channel3");

        SseJwtToken jwt = new SseJwtToken(9999999L, mockChannelList, JWT);

        when(mAuthResponse.isValidApiKey()).thenReturn(true);
        when(mAuthResponse.isStreamingEnabled()).thenReturn(true);
        when(mAuthResponse.getToken()).thenReturn(JWT);

        when(mFetcher.execute(any())).thenReturn(mAuthResponse);

        when(mJwtParser.parse(any())).thenReturn(jwt);

        SplitTaskExecutionInfo info = mTask.execute();

        verify(mFetcher, times(1)).execute(any());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, info.getStatus());

        SseJwtToken jwtToken = (SseJwtToken) info.getObjectValue(SplitTaskExecutionInfo.PARSED_SSE_JWT);
        Assert.assertEquals(3, jwtToken.getChannels().size());
        Assert.assertEquals("channel1", jwtToken.getChannels().get(0));
        Assert.assertEquals(true, info.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY));
        Assert.assertEquals(true, info.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED));
        Assert.assertEquals(9999999L, jwtToken.getExpirationTime());
    }

    @Test
    public void correctExecutionInvalidApiAndKey() throws HttpFetcherException, InvalidJwtTokenException {

        List<String> mockList = new ArrayList<>();
        SseJwtToken jwt = new SseJwtToken(9999999L, mockList, JWT);
        when(mAuthResponse.isValidApiKey()).thenReturn(false);
        when(mAuthResponse.isStreamingEnabled()).thenReturn(false);
        when(mAuthResponse.getToken()).thenReturn(null);

        when(mFetcher.execute(any())).thenReturn(mAuthResponse);

        when(mJwtParser.parse(any())).thenReturn(jwt);


        SplitTaskExecutionInfo info = mTask.execute();

        SseJwtToken jwtToken = (SseJwtToken) info.getObjectValue(SplitTaskExecutionInfo.PARSED_SSE_JWT);

        verify(mFetcher, times(1)).execute(any());
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, info.getStatus());
        Assert.assertEquals(0, jwtToken.getChannels().size());
        Assert.assertEquals(false, info.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY));
        Assert.assertEquals(false, info.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED));
        Assert.assertEquals(9999999L, jwtToken.getExpirationTime());
    }

    @Test
    public void fetcherException() throws HttpFetcherException {
        doThrow(NullPointerException.class).when(mFetcher).execute(any());

        SplitTaskExecutionInfo info = mTask.execute();
        Assert.assertEquals(SplitTaskExecutionStatus.ERROR, info.getStatus());
        Assert.assertNull(info.getStringValue(SplitTaskExecutionInfo.PARSED_SSE_JWT));
        Assert.assertNull(info.getBoolValue(SplitTaskExecutionInfo.IS_VALID_API_KEY));
        Assert.assertNull(info.getBoolValue(SplitTaskExecutionInfo.IS_STREAMING_ENABLED));
    }

    @After
    public void tearDown() {
        reset(mFetcher);
    }
}
