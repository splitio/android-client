package io.split.android.client.service.sseclient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.http.HttpSseAuthTokenFetcher;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticationResult;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class SseAuthenticatorTest {

    @Mock
    SseJwtParser mJwtParser;

    @Mock
    SseAuthenticationResponse mResponse;

    @Mock
    HttpSseAuthTokenFetcher mFetcher;

    List<String> mDummyChannels;

    String dummyKey = "CUSTOMER_ID";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mDummyChannels = Arrays.asList("channel1", "channel2");
    }

    @Test
    public void successfulRequest() throws InvalidJwtTokenException, HttpFetcherException {
        SseJwtToken token = new SseJwtToken(100, 200, 0, mDummyChannels, "the raw token");
        when(mResponse.isStreamingEnabled()).thenReturn(true);
        when(mResponse.getToken()).thenReturn("");

        when(mJwtParser.parse(anyString())).thenReturn(token);

        when(mFetcher.execute(any(), any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, dummyKey, mJwtParser);
        SseAuthenticationResult result = authenticator.authenticate();

        SseJwtToken respToken = result.getJwtToken();
        Assert.assertTrue(result.isPushEnabled());
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(token.getExpirationTime(), respToken.getExpirationTime());
        Assert.assertEquals(token.getChannels().size(), respToken.getChannels().size());
        Assert.assertEquals(token.getRawJwt(), respToken.getRawJwt());
    }

    @Test
    public void tokenParseError() throws InvalidJwtTokenException, HttpFetcherException {
        SseJwtToken token = new SseJwtToken(100, 200, 0, mDummyChannels, "the raw token");
        when(mResponse.isStreamingEnabled()).thenReturn(true);
        when(mResponse.getToken()).thenReturn("");

        when(mJwtParser.parse(anyString())).thenThrow(InvalidJwtTokenException.class);

        when(mFetcher.execute(any(), any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, dummyKey, mJwtParser);
        SseAuthenticationResult result = authenticator.authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertFalse(result.isSuccess());
        Assert.assertNull(result.getJwtToken());
    }

    @Test
    public void recoverableError() throws InvalidJwtTokenException, HttpFetcherException {
        SseJwtToken token = new SseJwtToken(100, 200, 0, Arrays.asList(), "the raw token");
        when(mResponse.isStreamingEnabled()).thenReturn(false);
        when(mResponse.getToken()).thenReturn(null);
        when(mResponse.isClientError()).thenReturn(false);

        when(mFetcher.execute(any(), any())).thenThrow(HttpFetcherException.class);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, dummyKey, mJwtParser);
        SseAuthenticationResult result = authenticator.authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.isErrorRecoverable());
        Assert.assertNull(result.getJwtToken());
    }

    @Test
    public void nonRrecoverableError() throws InvalidJwtTokenException, HttpFetcherException {
        SseJwtToken token = new SseJwtToken(100, 200, 0, Arrays.asList(), "the raw token");
        when(mResponse.isStreamingEnabled()).thenReturn(false);
        when(mResponse.getToken()).thenReturn(null);
        when(mResponse.isClientError()).thenReturn(true);

        when(mFetcher.execute(any(), any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, dummyKey, mJwtParser);
        SseAuthenticationResult result = authenticator.authenticate();

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertFalse(result.isSuccess());
        Assert.assertFalse(result.isErrorRecoverable());
        Assert.assertNull(result.getJwtToken());
    }
}
