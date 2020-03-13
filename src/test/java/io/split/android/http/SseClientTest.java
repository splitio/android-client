package io.split.android.http;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.network.sseclient.SseClient;
import io.split.android.client.network.sseclient.EventSourceListener;
import io.split.android.client.network.sseclient.NotificationParser;
import io.split.android.helpers.FileHelper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class SseClientTest {
    private MockWebServer mWebServer;
    final private static String TEST_URL = "/testSseUrl/";
    SseClient mSseClient;
    NotificationParser mNotificationParser;

    final static String JWT = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleUlkIiwidHlwIjoiSldUIn0.eyJvcmdJZCI6ImY3ZjAzNTIwLTVkZjctMTFlOC04NDc2LTBlYzU0NzFhM2NlYyIsImVudklkIjoiZjdmNjI4OTAtNWRmNy0xMWU4LTg0NzYtMGVjNTQ3MWEzY2VjIiwidXNlcktleXMiOlsiamF2aSJdLCJ4LWFibHktY2FwYWJpbGl0eSI6IntcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X01UY3dOVEkyTVRNME1nPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2xcIjpbXCJzdWJzY3JpYmVcIl19IiwieC1hYmx5LWNsaWVudElkIjoiY2xpZW50SWQiLCJleHAiOjE1ODM5NDc4MTIsImlhdCI6MTU4Mzk0NDIxMn0.bSkxugrXKLaJJkvlND1QEd7vrwqWiPjn77pkrJOl4t8";

    @Before
    public void setup() throws IOException {
    }

    @Test
    public void test() throws MalformedURLException, URISyntaxException, InterruptedException {
        // ************ WIP ******************
        //URI uri = mWebServer.url(TEST_URL).uri();
        //URI uri = new URI("https://streamdata.motwin.net/https://stockmarket.streamdata.io/prices?X-Sd-Token=MzNkZTYwN2ItYTZlMy00ODMzLWFiZWMtZTkxNTM0NjE4MWE1");

        URI uri = new URI("https://realtime.ably.io/event-stream?myChannel=MzM5Njc0ODcyNg%3D%3D_MTExMzgwNjgx_MTcwNTI2MTM0Mg%3D%3D_mySegments=&v=1.1&token=" + JWT);

        mSseClient = new SseClient(uri, new HttpClientImpl(), new NotificationParser(), new Listener());
        Thread.sleep(10000);

        mSseClient.disconnect();

        mSseClient = new SseClient(uri, new HttpClientImpl(), new NotificationParser(), new Listener());
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

    private class Listener implements EventSourceListener {
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
