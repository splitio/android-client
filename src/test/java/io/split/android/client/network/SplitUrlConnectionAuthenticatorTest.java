package io.split.android.client.network;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class SplitUrlConnectionAuthenticatorTest {

    private SplitUrlConnectionAuthenticator mAuthenticator;
    private SplitAuthenticator mSplitAuthenticator;

    @Before
    public void setUp() {
        mSplitAuthenticator = mock(SplitAuthenticator.class);
        mAuthenticator = new SplitUrlConnectionAuthenticator(mSplitAuthenticator);
    }

    @Test
    public void originalConnectionIsReturnedWhenAuthenticationResultIsNull() {
        when(mSplitAuthenticator.authenticate(any())).thenReturn(null);

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        HttpURLConnection authenticate = mAuthenticator.authenticate(mockConnection);

        assertSame(authenticate, mockConnection);
    }

    @Test
    public void headersFromAuthenticatedRequestAreAddedToConnection() {
        SplitAuthenticatedRequest authRequest = mock(SplitAuthenticatedRequest.class);
        Map<String, String> authHeaders = new HashMap<>();
        authHeaders.put("header", "value");
        authHeaders.put("header2", "value2");
        when(authRequest.getHeaders()).thenReturn(authHeaders);
        when(mSplitAuthenticator.authenticate(any())).thenReturn(authRequest);

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        mAuthenticator.authenticate(mockConnection);

        verify(mockConnection).addRequestProperty("header", "value");
        verify(mockConnection).addRequestProperty("header2", "value2");
    }

    @Test
    public void nullHeadersAreIgnored() {
        SplitAuthenticatedRequest authRequest = mock(SplitAuthenticatedRequest.class);
        Map<String, String> authHeaders = new HashMap<>();
        authHeaders.put("header", "value");
        authHeaders.put("header2", null);
        when(authRequest.getHeaders()).thenReturn(authHeaders);
        when(mSplitAuthenticator.authenticate(any())).thenReturn(authRequest);

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        mAuthenticator.authenticate(mockConnection);

        verify(mockConnection).addRequestProperty("header", "value");
        verify(mockConnection, times(0)).addRequestProperty("header2", null);
    }

    @Test
    public void nullKeysInHeadersAreIgnored() {
        SplitAuthenticatedRequest authRequest = mock(SplitAuthenticatedRequest.class);
        Map<String, String> authHeaders = new HashMap<>();
        authHeaders.put("header", "value");
        authHeaders.put(null, "value2");
        when(authRequest.getHeaders()).thenReturn(authHeaders);
        when(mSplitAuthenticator.authenticate(any())).thenReturn(authRequest);

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        mAuthenticator.authenticate(mockConnection);

        verify(mockConnection).addRequestProperty("header", "value");
        verify(mockConnection, times(0)).addRequestProperty(null, "value2");
    }

    @Test
    public void noHeadersAreAddedWhenHeadersFromAuthRequestIsNull() {
        SplitAuthenticatedRequest authRequest = mock(SplitAuthenticatedRequest.class);
        when(authRequest.getHeaders()).thenReturn(null);
        when(mSplitAuthenticator.authenticate(any())).thenReturn(authRequest);

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        mAuthenticator.authenticate(mockConnection);

        verify(mockConnection, times(0)).addRequestProperty(any(), any());
    }
}
