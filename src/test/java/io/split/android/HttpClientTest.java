package io.split.android;

import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

import org.apache.http.client.utils.URIBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.utils.Json;
import io.split.android.helpers.FileHelper;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class HttpClientTest {

        MockWebServer mWebServer;
        String mTrackRequestBody = null;

        @Before
        public void setup() throws IOException {
            setupServer();
        }

        @Test
        public void severalRequest() throws Exception {
            HttpClient client  = new HttpClientImpl();

            // Test dummy request and response
            HttpUrl url = mWebServer.url("/test1/");
            HttpRequest dummyReq = client.request(url.uri(), HttpClient.HTTP_GET);
            HttpResponse dummyResp = dummyReq.execute();

            // Test my segments
            url = mWebServer.url("/test2/");
            HttpRequest mySegmentsReq = client.request(url.uri(), HttpClient.HTTP_GET);
            HttpResponse mySegmentsResp = mySegmentsReq.execute();

            // Test split changes
            url = mWebServer.url("/test3/");
            HttpRequest splitChangeReq = client.request(url.uri(), HttpClient.HTTP_GET);
            HttpResponse splitChangeResp = splitChangeReq.execute();

            // Test empty response
            url = mWebServer.url("/test4/");
            HttpRequest emptyReq = client.request(url.uri(), HttpClient.HTTP_GET);
            HttpResponse emptyResp = emptyReq.execute();

            // Test post with response data
            url = mWebServer.url("/post_resp/");
            HttpRequest postDataReq = client.request(url.uri(), HttpClient.HTTP_POST);
            HttpResponse postDataResp = postDataReq.execute();

            // Test post no response data
            url = mWebServer.url("/post_no_resp/");
            HttpRequest postNoDataReq = client.request(url.uri(), HttpClient.HTTP_POST);
            HttpResponse postNoDataResp = postNoDataReq.execute();


            // Test limit wrong request
            url = mWebServer.url("/limit_wrong/");
            HttpRequest limitNoSuccessReq = client.request(url.uri(), HttpClient.HTTP_GET);
            HttpResponse limitNoSuccessResp = limitNoSuccessReq.execute();

            // Test bad request
            url = mWebServer.url("/bad/");
            HttpRequest badReq = client.request(url.uri(), HttpClient.HTTP_GET);
            HttpResponse badResp = badReq.execute();

            // Assert dummy request and response
            Assert.assertEquals(200, dummyResp.getHttpStatus());
            Assert.assertTrue(dummyResp.isSuccess());
            Assert.assertEquals("this is split test", dummyResp.getData());

            // Assert my segments
            List<MySegment> mySegments = parseMySegments(mySegmentsResp.getData());
            Assert.assertEquals(200, mySegmentsResp.getHttpStatus());
            Assert.assertEquals(2, mySegments.size());
            Assert.assertEquals("id1", mySegments.get(0).id);
            Assert.assertEquals("groupa", mySegments.get(0).name);
            Assert.assertEquals("id2", mySegments.get(1).id);
            Assert.assertEquals("groupb", mySegments.get(1).name);

            // Assert split changes
            SplitChange splitChange = Json.fromJson(splitChangeResp.getData(), SplitChange.class);
            Assert.assertEquals(200, splitChangeResp.getHttpStatus());
            Assert.assertTrue(splitChangeResp.isSuccess());
            Assert.assertEquals(-1, splitChange.since);
            Assert.assertEquals(1506703262916L, splitChange.till);
            Assert.assertEquals(30, splitChange.splits.size());

            // Assert empty response
            Assert.assertEquals(200, emptyResp.getHttpStatus());
            Assert.assertTrue(emptyResp.isSuccess());
            Assert.assertNull(emptyResp.getData());

            // Assert post non empty response
            Assert.assertEquals(201, postDataResp.getHttpStatus());
            Assert.assertTrue(postDataResp.isSuccess());
            Assert.assertEquals("{\"resp\": 1 }", postDataResp.getData());

            // Assert empty response
            Assert.assertEquals(201, postNoDataResp.getHttpStatus());
            Assert.assertTrue(postNoDataResp.isSuccess());
            Assert.assertNull(postNoDataResp.getData());

            // Assert limit wrong request
            Assert.assertEquals(300, limitNoSuccessResp.getHttpStatus());
            Assert.assertFalse(limitNoSuccessResp.isSuccess());
            Assert.assertNull(limitNoSuccessResp.getData());

            // Assert bad request
            Assert.assertEquals(400, badResp.getHttpStatus());
            Assert.assertFalse(badResp.isSuccess());
            Assert.assertNull(badResp.getData());
        }

        @After
        public void tearDown() throws IOException {
            mWebServer.shutdown();
        }

        private void setupServer() throws IOException {

            final String splitChangesResponse = new FileHelper().loadFileContent("split_changes_1.json");

            mWebServer = new MockWebServer();
            final Dispatcher dispatcher = new Dispatcher() {

                @Override
                public MockResponse dispatch (RecordedRequest request) throws InterruptedException {

                    switch (request.getPath()) {
                        case "/test1/":
                            return new MockResponse().setResponseCode(200).setBody("this is split test");

                        case "/test2/":

                            return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{\"id\":\"id1\", \"name\":\"groupa\"}, {\"id\":\"id2\", \"name\":\"groupb\"}]}");
                        case "/test3/":
                            return new MockResponse().setResponseCode(200).setBody(splitChangesResponse);

                        case "/test4/":
                            return new MockResponse().setResponseCode(200);

                        case "/post_resp/":
                            return new MockResponse().setResponseCode(201).setBody("{\"resp\": 1 }");

                        case "/post_no_resp/":
                            return new MockResponse().setResponseCode(201);

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
        }

        private void log(String m) {
            System.out.println("FACTORY_TEST: " + m);
        }

        private List<MySegment> parseMySegments(String json) {
            Type mapType = new TypeToken<Map<String, List<MySegment>>>() {
            }.getType();

            Map<String, List<MySegment>> mySegmentsMap = Json.fromJson(json, mapType);
            return mySegmentsMap.get("mySegments");
        }

    }

