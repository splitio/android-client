package io.split.android;

import junit.framework.Assert;

import org.apache.http.client.utils.URIBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;

import java.io.IOException;

import io.split.android.client.network.HttpRequest;
import io.split.android.client.network.HttpResponse;
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
            HttpUrl u = mWebServer.url("/test1/");
            HttpRequest request = client.request(u.uri(), HttpClient.HTTP_GET);
            HttpResponse response = request.execute();
            Assert.assertEquals(200, response.getHttpStatus());
            Assert.assertEquals("this is split test", response.getData());
        }

        @After
        public void tearDown() throws IOException {
            mWebServer.shutdown();
        }

        private void setupServer() throws IOException {
            mWebServer = new MockWebServer();
            final Dispatcher dispatcher = new Dispatcher() {

                @Override
                public MockResponse dispatch (RecordedRequest request) throws InterruptedException {

                    switch (request.getPath()) {
                        case "/test1/":
                            return new MockResponse().setResponseCode(200).setBody("this is split test");
                        case "/test2/":
                            return new MockResponse().setResponseCode(200).setBody("this\nis\nsplit\ntest");
                        case "/test3/":
                            return new MockResponse().setResponseCode(200).setBody("{\\\"info\\\":{\\\"name\":\"Lucas Albuquerque\",\"age\":\"21\",\"gender\":\"male\"}}");
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

    }

