package service;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.service.sseclient.EventStreamParser;
import io.split.android.client.service.sseclient.SseChannelsParser;
import io.split.android.client.service.sseclient.SseClient;
import io.split.android.client.service.sseclient.SseClientListener;
import okhttp3.mockwebserver.MockWebServer;

public class SseClientTest {
    private MockWebServer mWebServer;
    final private static String TEST_URL = "/testSseUrl/";
    SseClient mSseClient;
    EventStreamParser mEventStreamParser;

    final static String JWT = "eyJhbGciOiJIUzI1NiIsImtpZCI6IjVZOU05US45QnJtR0EiLCJ0eXAiOiJKV1QifQ.eyJvcmdJZCI6IiIsImVudklkIjoiIiwieC1hYmx5LWNhcGFiaWxpdHkiOiJ7XCJNek01TmpjME9EY3lOZz09X01URXhNemd3TmpneF9NVGN3TlRJMk1UTTBNZz09X215U2VnbWVudHNcIjpbXCJzdWJzY3JpYmVcIl0sXCJNek01TmpjME9EY3lOZz09X01URXhNemd3TmpneF9zcGxpdHNcIjpbXCJzdWJzY3JpYmVcIl0sXCJjb250cm9sXCI6W1wic3Vic2NyaWJlXCJdfSIsIngtYWJseS1jbGllbnRJZCI6ImNsaWVudElkIiwiZXhwIjoxNTg0NjUwNzU3LCJpYXQiOjE1ODQ2NDcxNTd9.71TJyNt4Mlp7KAyT1ARD2DyBzBVSvS06SZcNACbMjKM";

    @Before
    public void setup() throws IOException {
    }

    @Test
    public void okSse() throws MalformedURLException, URISyntaxException, InterruptedException, UnsupportedEncodingException {
        // ************ WIP ******************


        SseChannelsParser channelsParser = new SseChannelsParser();
        List<String> channelList = channelsParser.parse(JWT);
        String channels = String.join(",", channelList);

        //URI uri = new URI("https://realtime.ably.io/sse?v=1.1&channel=" + channels + "&accessToken=" + JWT);
        URI uri = new URI("https://realtime.ably.io/sse");

        HttpClient httpClient = new HttpClientImpl();

        httpClient.setHeader("Content-Type","text/event-stream");

        mSseClient = new SseClient(uri, httpClient, new EventStreamParser(), new Listener());
        mSseClient.connect(JWT, channelList);
        Thread.sleep(1000000);

        mSseClient.disconnect();

        mSseClient = new SseClient(uri, new HttpClientImpl(), new EventStreamParser(), new Listener());
        Thread.sleep(100000);
    }

    private void setupServer() throws IOException {

//        final String splitChangesResponse = new FileHelper().loadFileContent("split_changes_1.json");
//
//        mWebServer = new MockWebServer();
//        final Dispatcher dispatcher = new Dispatcher() {
//
//            @Override
//            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
//
//                switch (request.getPath()) {
//                    case TEST_URL:
//                        return new MockResponse().setResponseCode(200).setBody("this is split test");
//                }
//                return new MockResponse().setResponseCode(404);
//            }
//        };
//        mWebServer.setDispatcher(dispatcher);
//        mWebServer.start();
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

