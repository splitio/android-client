package io.split.android.client.service.sseclient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.service.sseclient.spi.StreamingAuthException;
import io.split.android.client.service.sseclient.spi.StreamingAuthFetcher;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticationResult;
import io.split.android.client.service.sseclient.sseclient.SseAuthenticator;

@SuppressWarnings("unchecked")
public class SseAuthenticatorTest {

    @Mock
    SseJwtParser mJwtParser;

    @Mock
    SseAuthenticationResponse mResponse;

    @Mock
    StreamingAuthFetcher mFetcher;

    List<String> mDummyChannels;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mDummyChannels = Arrays.asList("channel1", "channel2");
    }

    @Test
    public void successfulRequest() throws InvalidJwtTokenException, StreamingAuthException {
        SseJwtToken token = new SseJwtToken(100, 200, mDummyChannels, "the raw token");
        when(mResponse.isStreamingEnabled()).thenReturn(true);
        when(mResponse.getToken()).thenReturn("");

        when(mJwtParser.parse(anyString())).thenReturn(token);

        when(mFetcher.execute(any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser, null);
        SseAuthenticationResult result = authenticator.authenticate(60L);

        SseJwtToken respToken = result.getJwtToken();
        Assert.assertTrue(result.isPushEnabled());
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(token.getExpirationTime(), respToken.getExpirationTime());
        Assert.assertEquals(token.getChannels().size(), respToken.getChannels().size());
        Assert.assertEquals(token.getRawJwt(), respToken.getRawJwt());
    }

    @Test
    public void tokenParseError() throws InvalidJwtTokenException, StreamingAuthException {
        when(mResponse.isStreamingEnabled()).thenReturn(true);
        when(mResponse.getToken()).thenReturn("");

        when(mJwtParser.parse(anyString())).thenThrow(InvalidJwtTokenException.class);

        when(mFetcher.execute(any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser, null);
        SseAuthenticationResult result = authenticator.authenticate(60L);

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertFalse(result.isSuccess());
        Assert.assertNull(result.getJwtToken());
    }

    @Test
    public void recoverableError() throws StreamingAuthException {
        when(mResponse.isStreamingEnabled()).thenReturn(false);
        when(mResponse.getToken()).thenReturn(null);
        when(mResponse.isClientError()).thenReturn(false);

        when(mFetcher.execute(any())).thenThrow(StreamingAuthException.class);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser, null);
        SseAuthenticationResult result = authenticator.authenticate(60L);

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.isErrorRecoverable());
        Assert.assertNull(result.getJwtToken());
    }

    @Test
    public void nonRecoverableError() throws StreamingAuthException {
        when(mResponse.isStreamingEnabled()).thenReturn(false);
        when(mResponse.getToken()).thenReturn(null);
        when(mResponse.isClientError()).thenReturn(true);

        when(mFetcher.execute(any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser, null);
        SseAuthenticationResult result = authenticator.authenticate(60L);

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertFalse(result.isSuccess());
        Assert.assertFalse(result.isErrorRecoverable());
        Assert.assertNull(result.getJwtToken());
    }

    @Test
    public void registeredKeysAreUsedInFetcher() throws StreamingAuthException {
        when(mResponse.isClientError()).thenReturn(false);
        when(mFetcher.execute(any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser, null);
        authenticator.registerKey("user1");
        authenticator.registerKey("user2");
        Map<String, Object> map = new HashMap<>();
        Set<String> usersSet = new HashSet<>();
        usersSet.add("user1");
        usersSet.add("user2");
        map.put("users", usersSet);

        authenticator.authenticate(60L);

        verify(mFetcher).execute(map);
    }

    @Test
    public void unregisteredKeysAreNotUsedInFetcher() throws StreamingAuthException {
        when(mResponse.isClientError()).thenReturn(false);
        when(mFetcher.execute(any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser, null);
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

        verify(mFetcher).execute(map);
    }

    @Test
    public void flagsSpecIsUsedInFetcher() throws StreamingAuthException {
        when(mResponse.isClientError()).thenReturn(false);
        when(mFetcher.execute(any())).thenReturn(mResponse);

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser, "1.1");

        authenticator.authenticate(60L);

        verify(mFetcher).execute(argThat(argument -> {
            List<String> keys = new ArrayList<>(argument.keySet());
            return keys.get(0).equals("s") &&
                    keys.get(1).equals("users");
        }));
    }

    @Test
    public void flagsSpecIsNotUsedInFetcherWhenNull() throws StreamingAuthException {
        when(mResponse.isClientError()).thenReturn(false);
        when(mFetcher.execute(any())).thenReturn(mResponse);
        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser, null);

        authenticator.authenticate(60L);

        verify(mFetcher).execute(argThat(argument -> {
            List<String> keys = new ArrayList<>(argument.keySet());
            return keys.get(0).equals("users");
        }));
    }

    @Test
    public void flagsSpecIsNotUsedInFetcherWhenEmpty() throws StreamingAuthException {
        when(mResponse.isClientError()).thenReturn(false);
        when(mFetcher.execute(any())).thenReturn(mResponse);
        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser, "");

        authenticator.authenticate(60L);

        verify(mFetcher).execute(argThat(argument -> {
            List<String> keys = new ArrayList<>(argument.keySet());
            return keys.get(0).equals("users");
        }));
    }

    @Test
    public void returnUnrecoverableErrorWhenHttpStatusIsInternalNonRetryable() throws StreamingAuthException {

        when(mFetcher.execute(any())).thenThrow(new StreamingAuthException("error", null, 9009));

        SseAuthenticator authenticator = new SseAuthenticator(mFetcher, mJwtParser, null);
        SseAuthenticationResult result = authenticator.authenticate(60L);

        Assert.assertFalse(result.isPushEnabled());
        Assert.assertFalse(result.isSuccess());
        Assert.assertFalse(result.isErrorRecoverable());
        Assert.assertNull(result.getJwtToken());
    }
}
