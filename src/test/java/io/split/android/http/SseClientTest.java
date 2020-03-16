package io.split.android.http;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.service.sseclient.SseClient;
import io.split.android.client.service.sseclient.EventStreamParser;
import io.split.android.client.service.sseclient.SseClientListener;

import io.split.android.helpers.FileHelper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class SseClientTest {
    private MockWebServer mWebServer;
    final private static String TEST_URL = "/testSseUrl/";
    SseClient mSseClient;
    EventStreamParser mEventStreamParser;

    final static String JWT = "";

    @Before
    public void setup() throws IOException {
    }

    @Test
    public void test() throws MalformedURLException, URISyntaxException, InterruptedException, UnsupportedEncodingException {
        // ************ WIP ******************
        //URI uri = mWebServer.url(TEST_URL).uri();
        //URI uri = new URI("https://streamdata.motwin.net/https://stockmarket.streamdata.io/prices?X-Sd-Token=MzNkZTYwN2ItYTZlMy00ODMzLWFiZWMtZTkxNTM0NjE4MWE1");

        List<String> channelList = new ArrayList<>();


        String channels = String.join(",", channelList);

        URI uri = new URI("https://realtime.ably.io/sse?v=1.1&channel=" + channels + "&accessToken=" + JWT);

        HttpClient httpClient = new HttpClientImpl();
        
        httpClient.setHeader("Content-Type","text/event-stream");

        mSseClient = new SseClient(uri, httpClient, new EventStreamParser(), new Listener());
        Thread.sleep(100000);

        mSseClient.disconnect();

        mSseClient = new SseClient(uri, new HttpClientImpl(), new EventStreamParser(), new Listener());
        Thread.sleep(10000);
    }

    private void setupServer() throws IOException {

        final String splitChangesResponse = new FileHelper().loadFileContent("split_changes_1.json");

        mWebServer = new MockWebServer();
        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

                switch (request.getPath()) {
                    case TEST_URL:
                        return new MockResponse().setResponseCode(200).setBody("this is split test");
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        mWebServer.setDispatcher(dispatcher);
        mWebServer.start();
    }

    private class Listener implements SseClientListener {
        @Override
        public void onOpen() {
            System.out.println("SseClientTest: OnOPEN!!!!");
        }

        @Override
        public void onMessage(Map<String, String> values) {
            System.out.println("SseClientTest: OnMsg!!!!");
        }

        @Override
        public void onError() {
            System.out.println("SseClientTest: OnError!!!!");
        }
    }


}
