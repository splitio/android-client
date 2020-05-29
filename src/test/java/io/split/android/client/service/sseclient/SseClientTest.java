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
import io.split.android.client.service.sseclient.SseJwtParser;
import io.split.android.client.service.sseclient.SseClient;
import io.split.android.client.service.sseclient.SseClientListener;
import okhttp3.mockwebserver.MockWebServer;

public class SseClientTest {


    @Before
    public void setup() throws IOException {
    }

    @Test
    public void test() {



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
        public void onError(boolean isRecoverable) {
            System.out.println("SseClientTest: OnError!!!!");
        }

        @Override
        public void onKeepAlive() {

        }
    }


}

