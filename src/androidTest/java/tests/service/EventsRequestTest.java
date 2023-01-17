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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import helper.IntegrationHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.network.HttpClient;

public class EventsRequestTest {

    private SplitFactory mSplitFactory;
    private final AtomicReference<String> mEventsRequestBody = new AtomicReference<>("");
    private final CountDownLatch mEventsLatch = new CountDownLatch(1);

    @Before
    public void setUp() throws IOException {
        Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
        HttpClient httpClient = new HttpClientMock(IntegrationHelper.buildDispatcher(getMockResponses()));
        mSplitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                new Key("key1"),
                IntegrationHelper.basicConfig(),
                mContext,
                httpClient
        );
        mEventsRequestBody.set("");
    }

    @Test
    public void verifyResponseObjectContainsDesiredFields() throws InterruptedException {
        SplitClient client = mSplitFactory.client();

        client.track("test_event");
        Thread.sleep(1000);
        client.destroy();
        boolean await = mEventsLatch.await(10, TimeUnit.SECONDS);

        JsonObject expectedResponseJson = jsonFromResponse(mEventsRequestBody.get());

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

    private Map<String, IntegrationHelper.ResponseClosure> getMockResponses() {
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();

        responses.put("events/bulk", (uri, httpMethod, body) -> {
            mEventsRequestBody.set(body);
            mEventsLatch.countDown();

            return new HttpResponseMock(200, "{}");
        });

        return responses;
    }
}
