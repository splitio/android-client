package io.split.android.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpException;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.network.HttpResponseImpl;
import io.split.android.client.network.SdkTargetPath;
import io.split.android.client.network.URIBuilder;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.http.HttpFetcherImpl;
import io.split.android.client.service.http.HttpResponseParser;
import io.split.android.client.service.mysegments.MySegmentsResponseParser;
import io.split.android.client.service.splits.SplitChangeResponseParser;
import io.split.android.client.utils.NetworkHelper;

public class HttpFetcherTest {

    private final static String TEST_URL = "http://testurl.com";
    private final static String SPLIT_CHANGES_TEST_URL = TEST_URL + SdkTargetPath.SPLIT_CHANGES;
    private final static String MY_SEGMENTS_TEST_URL = TEST_URL + SdkTargetPath.MY_SEGMENTS;

    NetworkHelper mNetworkHelperMock;
    HttpClient mClientMock;
    URI mUrl;
    URI mSplitChangesUrl;
    URI mMySegmentsUrl;
    HttpResponseParser<SplitChange> mSplitChangeResponseParser = new SplitChangeResponseParser();
    HttpResponseParser<List<MySegment>> mMySegmentsResponseParser = new MySegmentsResponseParser();

    @Before
    public void setup() throws URISyntaxException {
        mUrl = new URI(TEST_URL);
        mSplitChangesUrl = new URI(SPLIT_CHANGES_TEST_URL);
        mMySegmentsUrl = new URIBuilder(new URI(MY_SEGMENTS_TEST_URL), "thekey").build();
        mNetworkHelperMock = mock(NetworkHelper.class);
        mClientMock = mock(HttpClient.class);
    }

    @Test
    public void testNoReachableUrl() throws URISyntaxException {

        when(mNetworkHelperMock.isReachable(mSplitChangesUrl)).thenReturn(false);

        HttpFetcher<SplitChange> fetcher = new HttpFetcherImpl<>(mClientMock, mUrl, mNetworkHelperMock, mSplitChangeResponseParser);
        boolean isReachable = true;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("since", -1);
            fetcher.execute(params, null);
        } catch (HttpFetcherException e) {
            isReachable = false;
        }

        Assert.assertFalse(isReachable);
    }

    @Test
    public void testSuccessfulSplitChangeFetch() throws URISyntaxException, HttpException {
        boolean exceptionWasThrown = false;
        URI uri = new URIBuilder(mSplitChangesUrl).addParameter("since", "" + -1).build();
        when(mNetworkHelperMock.isReachable(mSplitChangesUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, dummySplitChangeResponse());
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(uri, HttpMethod.GET, null, null)).thenReturn(request);

        HttpFetcher<SplitChange> fetcher = new HttpFetcherImpl<>(mClientMock, mSplitChangesUrl, mNetworkHelperMock, mSplitChangeResponseParser);
        SplitChange change = null;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("since", -1);
            change = fetcher.execute(params, null);
        } catch (HttpFetcherException e) {
            exceptionWasThrown = true;
        }

        Assert.assertFalse(exceptionWasThrown);
        Assert.assertEquals(1, change.splits.size());
        Assert.assertEquals("sample_feature", change.splits.get(0).name);
        Assert.assertEquals(-1, change.since);
        Assert.assertEquals(100, change.till);
    }

    @Test
    public void tesNonEmptyHeaderSplitChangesFetch() throws URISyntaxException, HttpException {
        boolean exceptionWasThrown = false;

        Map<String, String> headers = new HashMap<>();
        headers.put("header1", "value1");

        URI uri = new URIBuilder(mSplitChangesUrl).addParameter("since", "" + -1).build();
        when(mNetworkHelperMock.isReachable(mSplitChangesUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, dummySplitChangeResponse());
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(uri, HttpMethod.GET, null, headers)).thenReturn(request);

        HttpFetcher<SplitChange> fetcher = new HttpFetcherImpl<>(mClientMock, mSplitChangesUrl, mNetworkHelperMock, mSplitChangeResponseParser);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("since", -1);
            SplitChange change = fetcher.execute(params, headers);
        } catch (HttpFetcherException e) {
            exceptionWasThrown = true;
        }

        ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mClientMock).request(eq(uri), eq(HttpMethod.GET), eq(null), headersCaptor.capture());
        Assert.assertEquals("value1", headersCaptor.getValue().get("header1"));
    }

    @Test
    public void testFailedResponse() throws URISyntaxException, HttpException {
        URI uri = new URIBuilder(mSplitChangesUrl).addParameter("since", "" + -1).build();
        when(mNetworkHelperMock.isReachable(mSplitChangesUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(500, "");
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(uri, HttpMethod.GET, null, null)).thenReturn(request);

        HttpFetcher<SplitChange> fetcher = new HttpFetcherImpl<>(mClientMock, mSplitChangesUrl, mNetworkHelperMock, mSplitChangeResponseParser);
        SplitChange change = null;
        boolean failed = false;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("since", -1);
            change = fetcher.execute(params, null);
        } catch (HttpFetcherException e) {
            failed = true;
        }

        Assert.assertTrue(failed);
        Assert.assertNull(change);
    }

    @Test
    public void testWrongResponse() throws URISyntaxException, HttpException {
        URI uri = new URIBuilder(mSplitChangesUrl).addParameter("since", "" + -1).build();
        when(mNetworkHelperMock.isReachable(mSplitChangesUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, "");
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(uri, HttpMethod.GET, null, null)).thenReturn(request);

        HttpFetcher<SplitChange> fetcher = new HttpFetcherImpl<>(mClientMock, mSplitChangesUrl, mNetworkHelperMock, mSplitChangeResponseParser);
        SplitChange change = null;
        boolean failed = false;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("since", -1);
            change = fetcher.execute(params, null);
        } catch (HttpFetcherException e) {
            failed = true;
        }

        Assert.assertTrue(failed);
        Assert.assertNull(change);
    }

    @Test
    public void testSuccessfulMySegmentsFetch() throws URISyntaxException, HttpException {
        boolean exceptionWasThrown = false;

        when(mNetworkHelperMock.isReachable(mMySegmentsUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, dummyMySegmentsResponse());
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(mMySegmentsUrl, HttpMethod.GET, null, null)).thenReturn(request);

        HttpFetcher<List<MySegment>> fetcher = new HttpFetcherImpl<>(mClientMock, mMySegmentsUrl, mNetworkHelperMock, mMySegmentsResponseParser);
        List<MySegment> mySegments = null;
        try {
            mySegments = fetcher.execute(new HashMap<>(), null);
        } catch (HttpFetcherException e) {
            exceptionWasThrown = true;
        }

        Set<String> mySegmentsSet = mySegments.stream().map(mySegment -> mySegment.name).collect(Collectors.toSet());

        Assert.assertFalse(exceptionWasThrown);
        Assert.assertEquals(2, mySegments.size());
        Assert.assertNotNull(mySegmentsSet.contains("segment1"));
        Assert.assertNotNull(mySegmentsSet.contains("segment2"));
    }

    @Test
    public void tesNonEmptyHeadersMySegmentsFetch() throws URISyntaxException, HttpException {
        boolean exceptionWasThrown = false;

        Map<String, String> headers = new HashMap<>();
        headers.put("header1", "value1");

        when(mNetworkHelperMock.isReachable(mMySegmentsUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, dummyMySegmentsResponse());
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(mMySegmentsUrl, HttpMethod.GET, null, headers)).thenReturn(request);

        HttpFetcher<List<MySegment>> fetcher = new HttpFetcherImpl<>(mClientMock, mMySegmentsUrl, mNetworkHelperMock, mMySegmentsResponseParser);

        try {
            List<MySegment> mySegments = fetcher.execute(new HashMap<>(), headers);
        } catch (HttpFetcherException e) {
            exceptionWasThrown = true;
        }

        ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mClientMock).request(eq(mMySegmentsUrl), eq(HttpMethod.GET), any(), headersCaptor.capture());
        Assert.assertEquals("value1", headersCaptor.getValue().get("header1"));
    }

    @Test
    public void testHandleParserExceptionFetch() throws URISyntaxException, HttpException {
        boolean exceptionWasThrown = false;

        when(mNetworkHelperMock.isReachable(mMySegmentsUrl)).thenReturn(true);
        HttpRequest request = mock(HttpRequest.class);

        HttpResponse response = new HttpResponseImpl(200, "wrong response here");
        when(request.execute()).thenReturn(response);
        when(mClientMock.request(mMySegmentsUrl, HttpMethod.GET)).thenReturn(request);


        HttpFetcher<List<MySegment>> fetcher = new HttpFetcherImpl<>(mClientMock, mMySegmentsUrl, mNetworkHelperMock, mMySegmentsResponseParser);
        List<MySegment> mySegments = null;
        try {
            mySegments = fetcher.execute(new HashMap<>(), null);
        } catch (HttpFetcherException e) {
            exceptionWasThrown = true;
        }


        Assert.assertTrue(exceptionWasThrown);
    }

    private String dummySplitChangeResponse() {
        return "{\"splits\":[{\"name\":\"sample_feature\", \"status\":\"ACTIVE\"}],\n" +
                "  \"since\":-1,\n" +
                "  \"till\":100}";
    }

    private String dummyMySegmentsResponse() {
        return "{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id2\", \"name\":\"segment2\"}]}";
    }
}
