package io.split.android.client.service.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.service.sseclient.SseAuthenticationResponse;

public class HttpSseAuthTokenFetcherTest {

    @Mock
    private HttpClient mHttpClient;
    @Mock
    private HttpResponseParser<SseAuthenticationResponse> mResponseParser;
    private HttpSseAuthTokenFetcher fetcher;

    @Before
    public void setUp() throws URISyntaxException, HttpResponseParserException, HttpException {
        MockitoAnnotations.openMocks(this);
        fetcher = new HttpSseAuthTokenFetcher(mHttpClient, new URI("target"), mResponseParser);

        when(mResponseParser.parse(any())).thenReturn(new SseAuthenticationResponse());

        HttpResponse responseMock = mock(HttpResponse.class);
        when(responseMock.isSuccess()).thenReturn(true);

        HttpRequest requestMock = mock(HttpRequest.class);
        when(requestMock.execute()).thenReturn(responseMock);

        when(mHttpClient.request(any(), any())).thenReturn(requestMock);
    }

    @Test
    public void multipleParametersWithSameKeyAreCorrectlyInsertedInUrl() throws HttpFetcherException, URISyntaxException {
        Map<String, Object> map = new HashMap<>();
        Set<String> usersSet = new HashSet<>();
        usersSet.add("user1");
        usersSet.add("user2");
        map.put("users", usersSet);

        fetcher.execute(map, Collections.emptyMap());

        verify(mHttpClient).request(new URI("target?users=user1&users=user2"), HttpMethod.GET);
    }

    @Test
    public void multipleParametersWithDifferentKeyAreCorrectlyInsertedInUrl() throws HttpFetcherException, URISyntaxException {
        Map<String, Object> map = new HashMap<>();
        map.put("users", "user1");
        map.put("another_parameter", "null");

        fetcher.execute(map, Collections.emptyMap());

        verify(mHttpClient).request(new URI("target?another_parameter=null&users=user1"), HttpMethod.GET);
    }

    @Test
    public void parameterIsCorrectlyInsertedInUrl() throws HttpFetcherException, URISyntaxException {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("users", "user1");

        fetcher.execute(parameterMap, Collections.emptyMap());

        verify(mHttpClient).request(new URI("target?users=user1"), HttpMethod.GET);
    }

    @Test
    public void mixedParametersAreCorrectlyInsertedInUrl() throws HttpFetcherException, URISyntaxException {
        Map<String, Object> map = new HashMap<>();
        Set<String> usersSet = new HashSet<>();
        usersSet.add("user1");
        usersSet.add("user2");
        map.put("users", usersSet);
        map.put("another_parameter", "null");

        fetcher.execute(map, Collections.emptyMap());

        verify(mHttpClient).request(new URI("target?another_parameter=null&users=user1&users=user2"), HttpMethod.GET);
    }

    @Test
    public void parameterOrderIsHonored() throws HttpFetcherException, URISyntaxException {
        Map<String, Object> params = new LinkedHashMap<>();

        params.put("param1", "value1");
        params.put("param2", "value2");

        fetcher.execute(params, Collections.emptyMap());

        verify(mHttpClient).request(new URI("target?param1=value1&param2=value2"), HttpMethod.GET);
    }
}
