package io.split.android.engine.splits;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.service.mysegments.HttpMySegmentsFetcher;
import io.split.android.client.service.mysegments.MySegmentsFetcherV2;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.HttpResponseImpl;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.service.splits.HttpSplitFetcher;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.engine.metrics.Metrics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpMySegmentsFetcherTest {

    private final static String TEST_URL = "http://testurl.com";
    private final static String FULL_TEST_URL = TEST_URL + "/mySegments";
    NetworkHelper mNetworkHelperMock;
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

        MySegmentsFetcherV2 fetcher = HttpMySegmentsFetcher.create(mClientMock, mUrl, mMetricsMock, mNetworkHelperMock, "thekey");
        boolean isReachable = true;
        try {
            fetcher.execute();
        } catch (IllegalStateException e) {
            isReachable = false;
        }

        Assert.assertFalse(isReachable);
    }

    @Test
    public void testSuccessfulFetch() throws URISyntaxException, HttpException {
        boolean exceptionWasThrown = false;
        URI uri = new URIBuilder(mFullUrl, "thekey").build();
        when(mNetworkHelperMock.isReachable(mFullUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, dummyResponse());
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(uri, HttpMethod.GET)).thenReturn(request);

        MySegmentsFetcherV2 fetcher = HttpMySegmentsFetcher.create(mClientMock, mUrl, mMetricsMock, mNetworkHelperMock, "thekey");
        List<MySegment> mySegmentList = null;
        try {
            mySegmentList = fetcher.execute();
        } catch (IllegalStateException e) {
            exceptionWasThrown = true;
        }

        Set<String> mySegments = mySegmentList.stream().map(mySegment -> mySegment.name).collect(Collectors.toSet());
        Assert.assertFalse(exceptionWasThrown);
        Assert.assertEquals(3, mySegmentList.size());
        Assert.assertTrue(mySegments.contains("segment1"));
        Assert.assertTrue(mySegments.contains("segment2"));
        Assert.assertTrue(mySegments.contains("segment3"));
    }

    @Test
    public void testFailedResponse() throws URISyntaxException, HttpException {
        URI uri = new URIBuilder(mFullUrl, "thekey").build();
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

        MySegmentsFetcherV2 fetcher = HttpMySegmentsFetcher.create(mClientMock, mUrl, mMetricsMock, mNetworkHelperMock, "thekey");
        List<MySegment> mySegmentList = null;
        boolean failed = false;
        try {
            mySegmentList = fetcher.execute();
        } catch (IllegalStateException e) {
            failed = true;
        }

        Assert.assertTrue(failed);
        Assert.assertNull(mySegmentList);
    }

    private String dummyResponse() {
        return "{\"mySegments\":[" +
                "{ \"id\":\"id1\", \"name\":\"segment1\"}," +
                "{ \"id\":\"id2\", \"name\":\"segment2\"}," +
                "{ \"id\":\"id3\", \"name\":\"segment3\"}" +
                "]}";
    }
}
