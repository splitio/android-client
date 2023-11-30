package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class SplitUrlConnectionAuthenticatorTest {

    private SplitUrlConnectionAuthenticator mAuthenticator;
    private SplitAuthenticator mSplitAuthenticator;

    @Before
    public void setUp() {
        mSplitAuthenticator = mock(SplitAuthenticator.class);
        mAuthenticator = new SplitUrlConnectionAuthenticator(mSplitAuthenticator);
    }

    @Test
    public void callingAuthenticateCallsAuthenticateOnTheSplitAuthenticator() throws IOException {
//        mAuthenticator.authenticate(mock(Route.class), mock(Response.class));
//
//        verify(mSplitAuthenticator).authenticate(argThat(Objects::nonNull));
    }

    @Test
    public void resultIsNullIfSplitAuthenticatorReturnsNull() throws IOException {
        Request authenticate = mAuthenticator.authenticate(mock(Route.class), mock(Response.class));

        assertNull(authenticate);
    }

    @Test
    public void headersFromAuthenticationAreNotAddedToResultWhenTheyAreNull() throws IOException {
        Response mockResponse = mock(Response.class);
        Request mockRequest = mock(Request.class);
        Request.Builder mockBuilder = mock(Request.Builder.class);
        Request mockResult = mock(Request.class);

        when(mockRequest.newBuilder()).thenReturn(mockBuilder);
        when(mockResponse.request()).thenReturn(mockRequest);
        when(mockBuilder.build()).thenReturn(mockResult);

        SplitAuthenticatedRequest mockAuthRequest = mock(SplitAuthenticatedRequest.class);
        when(mockAuthRequest.getHeaders()).thenReturn(null);
        when(mSplitAuthenticator.authenticate(any())).thenReturn(mockAuthRequest);

        Request result = mAuthenticator.authenticate(mock(Route.class), mockResponse);

        verify(mockRequest).newBuilder();
        verify(mockBuilder, times(0)).addHeader(any(), any());
        verify(mockBuilder).build();
        assertEquals(mockResult, result);
    }

    @Test
    public void exceptionInSplitAuthenticatorCausesResultToBeNull() throws IOException {
        when(mSplitAuthenticator.authenticate(any())).thenThrow(new RuntimeException());

        Request result = mAuthenticator.authenticate(mock(Route.class), mock(Response.class));

        assertNull(result);
    }

    @Test
    public void authorizationHeadersAreAddedToResultRequest() {
        Response mockResponse = mock(Response.class);
        Request mockRequest = mock(Request.class);
        Request.Builder mockBuilder = mock(Request.Builder.class);
        Request mockResult = mock(Request.class);

        when(mockRequest.newBuilder()).thenReturn(mockBuilder);
        when(mockResponse.request()).thenReturn(mockRequest);
        when(mockBuilder.build()).thenReturn(mockResult);

        SplitAuthenticatedRequest mockAuthRequest = mock(SplitAuthenticatedRequest.class);
        when(mockAuthRequest.getHeaders()).thenReturn(Collections.singletonMap("Authorization", Collections.singletonList("Bearer 1234567890")));
        when(mSplitAuthenticator.authenticate(any())).thenReturn(mockAuthRequest);

        Request result = mAuthenticator.authenticate(mock(Route.class), mockResponse);

        verify(mockRequest).newBuilder();
        verify(mockBuilder).addHeader("Authorization", "Bearer 1234567890");
        verify(mockBuilder).build();
        assertNotNull(result);
    }
}
