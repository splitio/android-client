package io.split.android.client.service.sseclient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.http.HttpSseAuthTokenFetcher;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticationResult;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;

public class SseAuthenticatorTest {

    @Mock
    SseJwtParser mJwtParser;

    @Mock
    SseAuthenticationResponse mResponse;

    @Mock
    HttpSseAuthTokenFetcher mFetcher;

    List<String> mDummyChannels;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mDummyChannels = Arrays.asList("channel1", "channel2");
    }

    @Test
    public void successfulRequest() throws InvalidJwtTokenException, HttpFetcherException {
        SseJwtToken token = new SseJwtToken(100, 200, mDummyChannels, "the raw token");
        when(mResponse.isStreamingEnabled()).thenReturn(true);
        when(mResponse.getToken()).thenReturn("");

        when(mJwtParser.parse(anyString())).thenReturn(token);

        when(mFetcher.execute(any(), any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser);
        SseAuthenticationResult result = authenticator.authenticate(60L);

        SseJwtToken respToken = result.getJwtToken();
        Assert.assertTrue(result.isPushEnabled());
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(token.getExpirationTime(), respToken.getExpirationTime());
        Assert.assertEquals(token.getChannels().size(), respToken.getChannels().size());
        Assert.assertEquals(token.getRawJwt(), respToken.getRawJwt());
    }

    @Test
    public void tokenParseError() throws InvalidJwtTokenException, HttpFetcherException {
        when(mResponse.isStreamingEnabled()).thenReturn(true);
        when(mResponse.getToken()).thenReturn("");

        when(mJwtParser.parse(anyString())).thenThrow(InvalidJwtTokenException.class);

        when(mFetcher.execute(any(), any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser);
        SseAuthenticationResult result = authenticator.authenticate(60L);

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertFalse(result.isSuccess());
        Assert.assertNull(result.getJwtToken());
    }

    @Test
    public void recoverableError() throws HttpFetcherException {
        when(mResponse.isStreamingEnabled()).thenReturn(false);
        when(mResponse.getToken()).thenReturn(null);
        when(mResponse.isClientError()).thenReturn(false);

        when(mFetcher.execute(any(), any())).thenThrow(HttpFetcherException.class);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser);
        SseAuthenticationResult result = authenticator.authenticate(60L);

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.isErrorRecoverable());
        Assert.assertNull(result.getJwtToken());
    }

    @Test
    public void nonRecoverableError() throws HttpFetcherException {
        when(mResponse.isStreamingEnabled()).thenReturn(false);
        when(mResponse.getToken()).thenReturn(null);
        when(mResponse.isClientError()).thenReturn(true);

        when(mFetcher.execute(any(), any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser);
        SseAuthenticationResult result = authenticator.authenticate(60L);

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertFalse(result.isSuccess());
        Assert.assertFalse(result.isErrorRecoverable());
        Assert.assertNull(result.getJwtToken());
    }

    @Test
    public void registeredKeysAreUsedInFetcher() throws HttpFetcherException {
        when(mResponse.isClientError()).thenReturn(false);
        when(mFetcher.execute(any(), any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser);
        authenticator.registerKey("user1");
        authenticator.registerKey("user2");
        Map<String, Object> map = new HashMap<>();
        Set<String> usersSet = new HashSet<>();
        usersSet.add("user1");
        usersSet.add("user2");
        map.put("users", usersSet);

        authenticator.authenticate(60L);

        verify(mFetcher).execute(map, null);
    }

    @Test
    public void unregisteredKeysAreNotUsedInFetcher() throws HttpFetcherException {
        when(mResponse.isClientError()).thenReturn(false);
        when(mFetcher.execute(any(), any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser);
        authenticator.registerKey("user1");
        authenticator.registerKey("user2");
        authenticator.registerKey("user3");
        authenticator.unregisterKey("user1");
        Map<String, Object> map = new HashMap<>();
        Set<String> usersSet = new HashSet<>();
        usersSet.add("user2");
        usersSet.add("user3");
        map.put("users", usersSet);

        authenticator.authenticate(60L);

        verify(mFetcher).execute(map, null);
    }
}
