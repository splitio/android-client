package io.split.android.http;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.network.eventsource.EventSource;
import io.split.android.client.network.eventsource.EventSourceListener;
import io.split.android.client.network.eventsource.EventSourceStreamParser;
import io.split.android.helpers.FileHelper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class EventSourceTest {
    private MockWebServer mWebServer;
    final private static String TEST_URL = "/testSseUrl/";
    EventSource mEventSource;
    EventSourceStreamParser mEventSourceStreamParser;

    @Before
    public void setup() throws IOException {
        //setupServer();
    }

    @Test
    public void test() throws MalformedURLException, URISyntaxException, InterruptedException {
        //URI uri = mWebServer.url(TEST_URL).uri();
        URI uri = new URI("https://streamdata.motwin.net/https://stockmarket.streamdata.io/prices?X-Sd-Token=MzNkZTYwN2ItYTZlMy00ODMzLWFiZWMtZTkxNTM0NjE4MWE1");

        mEventSource = new EventSource(uri, new HttpClientImpl(), new EventSourceStreamParser(), new Listener());
        Thread.sleep(999999);
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

    private class Listener implements EventSourceListener {
        @Override
        public void onOpen() {
            System.out.println("EventSourceTest: OnOPEN!!!!");
        }

        @Override
        public void onMessage(Map<String, String> values) {
            System.out.println("EventSourceTest: OnMsg!!!!");
        }

        @Override
        public void onError() {
            System.out.println("EventSourceTest: OnError!!!!");
        }
    }


}
