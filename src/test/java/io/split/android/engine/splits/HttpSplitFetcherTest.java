package io.split.android.engine.splits;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.HttpResponseImpl;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.backend.splits.HttpSplitFetcher;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.engine.metrics.Metrics;

import static org.mockito.Mockito.*;

public class HttpSplitFetcherTest {

    private final static String TEST_URL = "http://testurl.com";
    private final static String FULL_TEST_URL = TEST_URL + "/splitChanges";
    NetworkHelper mNetworkHelperMock ;
    Metrics mMetricsMock;
    HttpClient mClientMock;
    URI mUrl;
    URI mFullUrl;

    @Before
    public void setup() throws URISyntaxException {
        mUrl = new URI(TEST_URL);
        mFullUrl = new URI(FULL_TEST_URL);
        mNetworkHelperMock = mock(NetworkHelper.class);
        mMetricsMock = mock(Metrics.class);
        mClientMock = mock(HttpClient.class);
    }

    @Test
    public void testNoReachableUrl() throws URISyntaxException {

        when(mNetworkHelperMock.isReachable(mFullUrl)).thenReturn(false);

        HttpSplitFetcher fetcher = HttpSplitFetcher.create(mClientMock, mUrl, mMetricsMock, mNetworkHelperMock);
        boolean isReachable = true;
        try {
            fetcher.execute(-1);
        } catch (IllegalStateException e) {
            isReachable = false;
        }

        Assert.assertFalse(isReachable);
    }

    @Test
    public void testSuccessfulFetch() throws URISyntaxException, HttpException {
        URI uri = new URIBuilder(mFullUrl).addParameter("since", "" + -1).build();
        when(mNetworkHelperMock.isReachable(mFullUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, dummyResponse());
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(uri, HttpMethod.GET)).thenReturn(request);

        HttpSplitFetcher fetcher = HttpSplitFetcher.create(mClientMock, mUrl, mMetricsMock, mNetworkHelperMock);
        SplitChange change = null;
        try {
            change = fetcher.execute(-1);
        } catch (IllegalStateException e) {
        }

        Assert.assertEquals(1, change.splits.size());
        Assert.assertEquals("sample_feature", change.splits.get(0).name);
        Assert.assertEquals(-1, change.since);
        Assert.assertEquals(100, change.till);
    }

    @Test
    public void testFailedResponse() throws URISyntaxException, HttpException {
        URI uri = new URIBuilder(mFullUrl).addParameter("since", "" + -1).build();
        when(mNetworkHelperMock.isReachable(mFullUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(500, "");
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(uri, HttpMethod.GET)).thenReturn(request);

        HttpSplitFetcher fetcher = HttpSplitFetcher.create(mClientMock, mUrl, mMetricsMock, mNetworkHelperMock);
        SplitChange change = null;
        boolean failed = false;
        try {
            change = fetcher.execute(-1);
        } catch (IllegalStateException e) {
            failed = true;
        }

        Assert.assertTrue(failed);
        Assert.assertNull(change);
    }

    @Test
    public void testWrongResponse() throws URISyntaxException, HttpException {
        URI uri = new URIBuilder(mFullUrl).addParameter("since", "" + -1).build();
        when(mNetworkHelperMock.isReachable(mFullUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, "");
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(uri, HttpMethod.GET)).thenReturn(request);

        HttpSplitFetcher fetcher = HttpSplitFetcher.create(mClientMock, mUrl, mMetricsMock, mNetworkHelperMock);
        SplitChange change = null;
        boolean failed = false;
        try {
            change = fetcher.execute(-1);
        } catch (IllegalStateException e) {
            failed = true;
        }

        Assert.assertTrue(failed);
        Assert.assertNull(change);
    }

    private String dummyResponse() {
        return "{\"splits\":[{\"name\":\"sample_feature\", \"status\":\"ACTIVE\"}],\n" +
                "  \"since\":-1,\n" +
                "  \"till\":100}";
    }
}
