package tests.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.IntegrationHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpMethod;
import io.split.sharedtest.fake.HttpStreamResponseMock;

public class EventsRequestTest {

    private SplitFactory mSplitFactory;
    private String mEventsRequestBody;
    private CountDownLatch mEventsLatch;

    @Before
    public void setUp() throws IOException {
        Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
        HttpClient httpClient = new HttpClientMock(buildDispatcher());
        mSplitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                new Key("key1"),
                IntegrationHelper.basicConfig(),
                mContext,
                httpClient
        );
        mEventsRequestBody = "";
        mEventsLatch = new CountDownLatch(1);
    }

    @Test
    public void verifyResponseObjectContainsDesiredFields() throws InterruptedException {
        SplitClient client = mSplitFactory.client();

        client.track("test_event");
        client.destroy();
        boolean await = mEventsLatch.await(5, TimeUnit.SECONDS);

        JsonObject expectedResponseJson = jsonFromResponse(mEventsRequestBody);

        assertTrue(await);
        assertNotNull(expectedResponseJson.get("eventTypeId"));
        assertNotNull(expectedResponseJson.get("key"));
        assertNotNull(expectedResponseJson.get("timestamp"));
        assertNotNull(expectedResponseJson.get("trafficTypeName"));
        assertNotNull(expectedResponseJson.get("value"));
        assertNotNull(expectedResponseJson.get("properties"));
        assertNull(expectedResponseJson.get("sizeInBytes"));
        assertNull(expectedResponseJson.get("storageId"));
    }

    private JsonObject jsonFromResponse(String body) {
        JsonArray asJsonArray = JsonParser.parseString(body).getAsJsonArray();
        JsonElement jsonElement = asJsonArray.get(0);

        return jsonElement.getAsJsonObject();
    }

    private HttpResponseMockDispatcher buildDispatcher() {
        return new HttpResponseMockDispatcher() {
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("events")) {
                    mEventsRequestBody = body;
                    mEventsLatch.countDown();
                }
                return new HttpResponseMock(200, null);
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    return new HttpStreamResponseMock(200, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };
    }
}
