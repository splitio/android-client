package io.split.android.client.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.utils.Json;
import io.split.android.helpers.FileHelper;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class HttpClientTest {

    private MockWebServer mWebServer;
    private MockWebServer mProxyServer;
    private HttpClient client;
    private UrlSanitizer mUrlSanitizerMock;

    @Before
    public void setup() throws IOException {
        mUrlSanitizerMock = mock(UrlSanitizer.class);
        when(mUrlSanitizerMock.getUrl(any())).thenAnswer(new Answer<URL>() {
            @Override
            public URL answer(InvocationOnMock invocation) throws Throwable {
                URI argument = invocation.getArgument(0);

                return new URL(argument.toString());
            }
        });
        setupServer();
    }

    @Test
    public void severalRequest() throws Exception {

        // Test dummy request and response
        HttpUrl url = mWebServer.url("/test1/");
        HttpRequest dummyReq = client.request(url.uri(), HttpMethod.GET);
        HttpResponse dummyResp = dummyReq.execute();
        mWebServer.takeRequest();

        // Test my segments
        url = mWebServer.url("/test2/");
        HttpRequest mySegmentsReq = client.request(url.uri(), HttpMethod.GET);
        HttpResponse mySegmentsResp = mySegmentsReq.execute();
        mWebServer.takeRequest();

        // Test split changes
        url = mWebServer.url("/test3/");
        HttpRequest splitChangeReq = client.request(url.uri(), HttpMethod.GET);
        HttpResponse splitChangeResp = splitChangeReq.execute();
        mWebServer.takeRequest();

        // Test empty response
        url = mWebServer.url("/test4/");
        HttpRequest emptyReq = client.request(url.uri(), HttpMethod.GET);
        HttpResponse emptyResp = emptyReq.execute();
        mWebServer.takeRequest();

        // Test post with response data
        url = mWebServer.url("/post_resp/");
        HttpRequest postDataReq = client.request(url.uri(), HttpMethod.POST, "{}");
        HttpResponse postDataResp = postDataReq.execute();
        mWebServer.takeRequest();

        // Test post no response data
        url = mWebServer.url("/post_no_resp/");
        HttpRequest postNoDataReq = client.request(url.uri(), HttpMethod.POST, "{}");
        HttpResponse postNoDataResp = postNoDataReq.execute();
        mWebServer.takeRequest();

        // Test post tracks
        String postTracksData = new FileHelper().loadFileContent("tracks_1.json");
        url = mWebServer.url("/tracks/");
        HttpRequest postTracksReq = client.request(url.uri(), HttpMethod.POST, postTracksData);
        HttpResponse postTracksResp = postTracksReq.execute();
        RecordedRequest postTrackRecReq = mWebServer.takeRequest();
        String receivedPostTrackData = postTrackRecReq.getBody().readUtf8();
        List<Event> trackEventsSent = parseTrackEvents(receivedPostTrackData);

        // Test post impressions
        String postImpData = new FileHelper().loadFileContent("impressions_1.json");
        url = mWebServer.url("/impressions/");
        HttpRequest postImpReq = client.request(url.uri(), HttpMethod.POST, postImpData);
        HttpResponse postImpResp = postImpReq.execute();
        RecordedRequest postImpRecReq = mWebServer.takeRequest();
        String receivedPostImpData = postImpRecReq.getBody().readUtf8();
        List<TestImpressions> impSent = parseImpressions(receivedPostImpData);

        // Test limit wrong request
        url = mWebServer.url("/limit_wrong/");
        HttpRequest limitNoSuccessReq = client.request(url.uri(), HttpMethod.GET);
        HttpResponse limitNoSuccessResp = limitNoSuccessReq.execute();
        mWebServer.takeRequest();

        // Test bad request
        url = mWebServer.url("/bad/");
        HttpRequest badReq = client.request(url.uri(), HttpMethod.GET);
        HttpResponse badResp = badReq.execute();
        mWebServer.takeRequest();

        // Assert dummy request and response
        Assert.assertEquals(200, dummyResp.getHttpStatus());
        assertTrue(dummyResp.isSuccess());
        Assert.assertEquals("this is split test", dummyResp.getData());

        // Assert my segments
        List<String> mySegments = parseMySegments(mySegmentsResp.getData()).getSegmentsChange().getNames();
        Assert.assertEquals(200, mySegmentsResp.getHttpStatus());
        Assert.assertEquals(2, mySegments.size());
        Assert.assertTrue(mySegments.contains("groupa"));
        Assert.assertTrue(mySegments.contains("groupb"));

        // Assert split changes
        SplitChange splitChange = Json.fromJson(splitChangeResp.getData(), TargetingRulesChange.class).getFeatureFlagsChange();
        Assert.assertEquals(200, splitChangeResp.getHttpStatus());
        assertTrue(splitChangeResp.isSuccess());
        Assert.assertEquals(-1, splitChange.since);
        Assert.assertEquals(1506703262916L, splitChange.till);
        Assert.assertEquals(31, splitChange.splits.size());

        // Assert empty response
        Assert.assertEquals(200, emptyResp.getHttpStatus());
        assertTrue(emptyResp.isSuccess());
        Assert.assertNull(emptyResp.getData());

        // Assert post non empty response
        Assert.assertEquals(201, postDataResp.getHttpStatus());
        assertTrue(postDataResp.isSuccess());
        Assert.assertEquals("{\"resp\": 1 }", postDataResp.getData());

        // Assert empty response
        Assert.assertEquals(201, postNoDataResp.getHttpStatus());
        assertTrue(postNoDataResp.isSuccess());
        Assert.assertNull(postNoDataResp.getData());

        // Assert tracks
        Assert.assertEquals(200, postTracksResp.getHttpStatus());
        assertTrue(postTracksResp.isSuccess());
        Assert.assertNull(postTracksResp.getData());
        Assert.assertEquals(10, trackEventsSent.size());
        Assert.assertEquals("open_web", trackEventsSent.get(0).eventTypeId);
        Assert.assertEquals("CUSTOMER_ID", trackEventsSent.get(0).key);
        Assert.assertEquals(1.0, trackEventsSent.get(0).value, 0.0);
        Assert.assertEquals("by_shirt", trackEventsSent.get(9).eventTypeId);

        // Assert impressions
        Assert.assertEquals(200, postImpResp.getHttpStatus());
        assertTrue(postImpResp.isSuccess());
        Assert.assertNull(postImpResp.getData());
        Assert.assertEquals(2, impSent.size());
        Assert.assertEquals("ANDROID_sameTreatmentWithBucketingKey", impSent.get(0).testName);
        Assert.assertEquals(4, impSent.get(0).keyImpressions.size());
        Assert.assertEquals("sample_feature", impSent.get(1).testName);
        Assert.assertEquals(5, impSent.get(1).keyImpressions.size());

        // Assert limit wrong request
        Assert.assertEquals(300, limitNoSuccessResp.getHttpStatus());
        Assert.assertFalse(limitNoSuccessResp.isSuccess());
        Assert.assertNull(limitNoSuccessResp.getData());

        // Assert bad request
        Assert.assertEquals(400, badResp.getHttpStatus());
        Assert.assertFalse(badResp.isSuccess());
        Assert.assertNull(badResp.getData());
    }

    @Test
    public void addHeaders() throws InterruptedException, URISyntaxException, HttpException {
        client.addHeaders(Collections.singletonMap("my_header", "my_header_value"));

        HttpUrl url = mWebServer.url("/test1/");
        HttpRequest dummyReq = client.request(url.uri(), HttpMethod.GET);
        dummyReq.execute();
        RecordedRequest recReq = mWebServer.takeRequest();

        Headers headers = recReq.getHeaders();

        assertTrue(headers.names().contains("my_header"));
        assertEquals("my_header_value", headers.get("my_header"));
    }

    @Test
    public void addStreamingHeaders() throws InterruptedException, URISyntaxException, HttpException {
        client.addStreamingHeaders(Collections.singletonMap("my_header", "my_header_value"));

        HttpUrl url = mWebServer.url("/test1/");
        HttpStreamRequest dummyReq = client.streamRequest(url.uri());
        dummyReq.execute();
        RecordedRequest recReq = mWebServer.takeRequest();

        Headers headers = recReq.getHeaders();

        assertTrue(headers.names().contains("my_header"));
        assertEquals("my_header_value", headers.get("my_header"));
    }

    @Test
    public void requestWithNewHeaders() throws HttpException, InterruptedException {
        client.addHeaders(Collections.singletonMap("my_header", "my_header_value"));

        HttpUrl url = mWebServer.url("/test1/");
        HttpRequest dummyReq = client.request(url.uri(), HttpMethod.GET, "{}", Collections.singletonMap("new_header", "new_header_value"));
        dummyReq.execute();
        RecordedRequest recReq = mWebServer.takeRequest();

        Headers headers = recReq.getHeaders();

        assertTrue(headers.names().contains("my_header"));
        assertEquals("my_header_value", headers.get("my_header"));

        assertTrue(headers.names().contains("new_header"));
        assertEquals("new_header_value", headers.get("new_header"));
    }

    @Test
    public void addInvalidHeaderValue() {
        assertThrows(IllegalArgumentException.class, () -> client.addHeaders(Collections.singletonMap(null, "my_header_value")));
        assertThrows(IllegalArgumentException.class, () -> client.addHeaders(Collections.singletonMap("my_hader", null)));
    }

    @Test
    public void addInvalidStreamingHeaderValue() {
        assertThrows(IllegalArgumentException.class, () -> client.addStreamingHeaders(Collections.singletonMap(null, "my_header_value")));
        assertThrows(IllegalArgumentException.class, () -> client.addStreamingHeaders(Collections.singletonMap("my_hader", null)));
    }

    @Test
    public void testRequestWithProxy() throws HttpException, InterruptedException, IOException {
        mProxyServer = new MockWebServer();
        mProxyServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                System.out.println("Received request in proxy: ["+ request.toString() +"]");
                return new MockResponse().setResponseCode(200);
            }
        });
        mProxyServer.start();

        HttpClient client = new HttpClientImpl.Builder()
                .setContext(mock(Context.class))
                .setUrlSanitizer(mUrlSanitizerMock)
                .setProxy(HttpProxy.newBuilder(mProxyServer.getHostName(), mProxyServer.getPort()).build())
                .build();

        HttpRequest request = client.request(mWebServer.url("/test1/").uri(), HttpMethod.GET);
        HttpResponse execute = request.execute();
        RecordedRequest recordedRequest = mProxyServer.takeRequest();

        assertTrue(execute.isSuccess());
        assertEquals("GET", recordedRequest.getMethod());
        mProxyServer.shutdown();
    }

    @Test
    public void getRequestsThatRequireProxyAuthenticationAreRetried() throws IOException, HttpException, InterruptedException {
        CountDownLatch authLatch = new CountDownLatch(1);
        CountDownLatch failureLatch = new CountDownLatch(1);
        CountDownLatch successLatch = new CountDownLatch(1);
        mProxyServer = new MockWebServer();
        mProxyServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getHeader("Proxy-Authorization") == null) {
                    failureLatch.countDown();
                    return new MockResponse().setResponseCode(407);
                }

                successLatch.countDown();
                return new MockResponse().setResponseCode(200);
            }
        });
        mProxyServer.start();

        HttpClient client = new HttpClientImpl.Builder()
                .setContext(mock(Context.class))
                .setUrlSanitizer(mUrlSanitizerMock)
                .setProxyAuthenticator(new SplitAuthenticator() {
                    @Override
                    public SplitAuthenticatedRequest authenticate(@NonNull SplitAuthenticatedRequest request) {
                        authLatch.countDown();
                        request.setHeader("Proxy-Authorization", "my-auth");

                        return request;
                    }
                })
                .setProxy(HttpProxy.newBuilder(mProxyServer.getHostName(), mProxyServer.getPort()).build())
                .build();

        HttpRequest request = client.request(mWebServer.url("/test1/").uri(), HttpMethod.GET);
        request.execute();
        RecordedRequest recordedRequest1 = mProxyServer.takeRequest();
        RecordedRequest recordedRequest2 = mProxyServer.takeRequest();

        boolean failureAwait = failureLatch.await(5, TimeUnit.SECONDS);
        boolean authAwait = authLatch.await(5, TimeUnit.SECONDS);
        boolean successAwait = successLatch.await(5, TimeUnit.SECONDS);

        assertNotSame(recordedRequest1, recordedRequest2);
        assertNull(recordedRequest1.getHeader("Proxy-Authorization"));
        assertNotNull(recordedRequest2.getHeader("Proxy-Authorization"));
        assertEquals("GET", recordedRequest1.getMethod());
        assertEquals("GET", recordedRequest2.getMethod());
        assertTrue(failureAwait);
        assertTrue(authAwait);
        assertTrue(successAwait);
        mProxyServer.shutdown();
    }

    @Test
    public void postRequestsThatRequireProxyAuthenticationAreRetried() throws IOException, HttpException, InterruptedException {
        CountDownLatch authLatch = new CountDownLatch(1);
        CountDownLatch failureLatch = new CountDownLatch(1);
        CountDownLatch successLatch = new CountDownLatch(1);
        mProxyServer = new MockWebServer();
        mProxyServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getHeader("Proxy-Authorization") == null) {
                    failureLatch.countDown();
                    return new MockResponse().setResponseCode(407);
                }

                successLatch.countDown();
                return new MockResponse().setResponseCode(200);
            }
        });
        mProxyServer.start();

        HttpClient client = new HttpClientImpl.Builder()
                .setContext(mock(Context.class))
                .setUrlSanitizer(mUrlSanitizerMock)
                .setProxyAuthenticator(new SplitAuthenticator() {
                    @Override
                    public SplitAuthenticatedRequest authenticate(@NonNull SplitAuthenticatedRequest request) {
                        authLatch.countDown();
                        request.setHeader("Proxy-Authorization", "my-auth");

                        return request;
                    }
                })
                .setProxy(HttpProxy.newBuilder(mProxyServer.getHostName(), mProxyServer.getPort()).build())
                .build();

        HttpRequest request = client.request(mWebServer.url("/test1/").uri(), HttpMethod.POST, "{}");
        request.execute();
        RecordedRequest recordedRequest1 = mProxyServer.takeRequest();
        RecordedRequest recordedRequest2 = mProxyServer.takeRequest();

        boolean failureAwait = failureLatch.await(5, TimeUnit.SECONDS);
        boolean authAwait = authLatch.await(5, TimeUnit.SECONDS);
        boolean successAwait = successLatch.await(5, TimeUnit.SECONDS);

        assertNotSame(recordedRequest1, recordedRequest2);
        assertNull(recordedRequest1.getHeader("Proxy-Authorization"));
        assertNotNull(recordedRequest2.getHeader("Proxy-Authorization"));
        assertEquals("POST", recordedRequest1.getMethod());
        assertEquals("POST", recordedRequest2.getMethod());
        assertTrue(failureAwait);
        assertTrue(authAwait);
        assertTrue(successAwait);
        mProxyServer.shutdown();
    }

    @After
    public void tearDown() throws IOException {
        mWebServer.shutdown();
        client.close();
    }

    private void setupServer() throws IOException {

        final String splitChangesResponse = new FileHelper().loadFileContent("split_changes_1.json");

        mWebServer = new MockWebServer();
        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

                System.out.println("Sending request to: ["+ request.toString() +"]");

                switch (request.getPath()) {
                    case "/test1/":
                        return new MockResponse().setResponseCode(200).setBody("this is split test");

                    case "/test2/":

                        return new MockResponse().setResponseCode(200).setBody("{\"ms\":{\"k\":[{\"n\":\"groupa\"},{\"n\":\"groupb\"}],\"cn\":999999},\"ls\":{\"k\":[],\"cn\":999999}}");
                    case "/test3/":
                        return new MockResponse().setResponseCode(200).setBody(splitChangesResponse);

                    case "/test4/":
                        return new MockResponse().setResponseCode(200);

                    case "/post_resp/":
                        return new MockResponse().setResponseCode(201).setBody("{\"resp\": 1 }");

                    case "/post_no_resp/":
                        return new MockResponse().setResponseCode(201);

                    case "/tracks/":
                        return new MockResponse().setResponseCode(200);

                    case "/impressions/":
                        return new MockResponse().setResponseCode(200);

                    case "/limit_wrong/":
                        return new MockResponse().setResponseCode(300);

                    case "/bad/":
                        return new MockResponse().setResponseCode(400);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(dispatcher);
        mWebServer.start();

        client = new HttpClientImpl.Builder()
                .setUrlSanitizer(mUrlSanitizerMock)
                .build();
    }

    private AllSegmentsChange parseMySegments(String json) {
        return Json.fromJson(json, AllSegmentsChange.class);
    }

    private List<Event> parseTrackEvents(String json) {
        Type mapType = new TypeToken<List<Event>>() {
        }.getType();

        return Json.fromJson(json, mapType);
    }

    private List<TestImpressions> parseImpressions(String json) {
        Type mapType = new TypeToken<List<TestImpressions>>() {
        }.getType();

        return Json.fromJson(json, mapType);
    }
}
