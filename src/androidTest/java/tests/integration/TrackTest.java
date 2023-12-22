package tests.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import helper.DatabaseHelper;
import helper.ImpressionListenerHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Event;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.exceptions.SplitInstantiationException;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class TrackTest {

    Context mContext;
    MockWebServer mWebServer;
    int mCurReqId = 0;
    ArrayList<String> mJsonChanges = null;
    ArrayList<CountDownLatch> mLatchs;
    ArrayList<List<Event>> mEventsHits;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mCurReqId = 0;
        mLatchs = new ArrayList<>();
        mEventsHits = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            mLatchs.add(new CountDownLatch(1));
        }
        setupServer();
    }

    @After
    public void tearDown() throws IOException {
        mWebServer.shutdown();
    }

    private void setupServer() {
        mWebServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[]}");

                } else if (request.getPath().contains("/splitChanges")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody(emptyChanges());

                } else if (request.getPath().contains("/events/bulk")) {

                    int code = 0;
                    synchronized (this) {
                        int index = mCurReqId;
                        if (index > 0 && index < 4) {
                            code = 500;
                        } else {
                            List<Event> data = IntegrationHelper.buildEventsFromJson(request.getBody().readUtf8());
                            mEventsHits.add(data);
                            code = 200;
                        }

                        if (index < 6) {
                            mCurReqId = index + 1;
                            mLatchs.get(index).countDown();
                        }
                    }
                    Thread.sleep(300);
                    return new MockResponse().setResponseCode(code);

                } else if (request.getPath().contains("/testImpressions/bulk")) {
                    return new MockResponse().setResponseCode(200);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    @Test
    public void test() throws Exception {
        ArrayList<Integer> trackCount = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        String apiKey = IntegrationHelper.dummyApiKey();
        SplitRoomDatabase splitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        splitRoomDatabase.clearAllTables();
        splitRoomDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.DATBASE_MIGRATION_STATUS, GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE));

        ImpressionListenerHelper impListener = new ImpressionListenerHelper();

        SplitClient client;

        final String url = mWebServer.url("/").url().toString();

        Key key = new Key("CUSTOMER_ID", null);
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .serviceEndpoints(ServiceEndpoints.builder().apiEndpoint(url).eventsEndpoint(url).build())
                .ready(30000)
                .eventFlushInterval(5)
                .eventsPerPush(5)
                .eventsQueueSize(1000)
                .enableDebug()
                .trafficType("client")
                .impressionListener(impListener)
                .build();

        SplitFactory splitFactory = SplitFactoryBuilder.build(apiKey, key, config, mContext);

        client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        latch.await(20, TimeUnit.SECONDS);

        Map<String, Object> prop = new HashMap<>();

        client.track("custom", "e%%%%%");
        client.track("custom", "");
        client.track("custom", "e^^^^");

        for (int i = 0; i < 5; i++) {
            prop.put("value", 0.0);
            client.track("custom", "event1", i, prop);
        }
        mLatchs.get(0).await(20, TimeUnit.SECONDS);
        trackCount.add(mEventsHits.size());

        for (int i = 0; i < 5; i++) {
            prop.put("value", 2);
            client.track("custom", "event2", i, prop);
        }
        mLatchs.get(1).await(20, TimeUnit.SECONDS);
        trackCount.add(mEventsHits.size());

        for (int i = 0; i < 5; i++) {
            prop.put("value", 3);
            client.track("custom", "event3", i, prop);
        }
        mLatchs.get(2).await(20, TimeUnit.SECONDS);
        mLatchs.get(3).await(20, TimeUnit.SECONDS);
        trackCount.add(mEventsHits.size());

        mLatchs.get(4).await(20, TimeUnit.SECONDS);
        mLatchs.get(5).await(20, TimeUnit.SECONDS);
        trackCount.add(mEventsHits.size());

        client.destroy();

        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertEquals(1, trackCount.get(0).intValue());
        Assert.assertEquals(1, trackCount.get(1).intValue());
        Assert.assertEquals(1, trackCount.get(2).intValue());
        Assert.assertEquals(3, trackCount.get(3).intValue());

        Thread.sleep(2000);

        Event e1 = findEvent("event1", 0.0);
        Event e2 = findEvent("event2", 2.0);
        Event e3 = findEvent("event3", 3.0);

        Assert.assertEquals("custom", e1.trafficTypeName);
        Assert.assertEquals(0.0, e1.value, 0.0);
        Assert.assertEquals("event1", e1.eventTypeId);
        Assert.assertEquals(0, (e1.properties.get("value")));

        Assert.assertEquals("custom", e2.trafficTypeName);
        Assert.assertEquals(2.0, e2.value, 0.0);
        Assert.assertEquals("event2", e2.eventTypeId);
        Assert.assertEquals(2, (e2.properties.get("value")));

        Assert.assertEquals("custom", e3.trafficTypeName);
        Assert.assertEquals(3.0, e3.value, 0.0);
        Assert.assertEquals("event3", e3.eventTypeId);
        Assert.assertEquals(3, (e3.properties.get("value")));
    }

    @Test
    public void largeNumberInPropertiesTest() throws InterruptedException, SplitInstantiationException {
        CountDownLatch latch = new CountDownLatch(1);
        String apiKey = IntegrationHelper.dummyApiKey();
        SplitRoomDatabase splitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        splitRoomDatabase.clearAllTables();
        splitRoomDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.DATBASE_MIGRATION_STATUS, GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE));

        final String url = mWebServer.url("/").url().toString();

        Key key = new Key("CUSTOMER_ID", null);
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .serviceEndpoints(ServiceEndpoints
                        .builder()
                        .apiEndpoint(url)
                        .eventsEndpoint(url)
                        .build())
                .ready(30000)
                .eventFlushInterval(5)
                .eventsPerPush(5)
                .eventsQueueSize(1000)
                .enableDebug()
                .trafficType("client")
                .build();

        SplitFactory splitFactory = SplitFactoryBuilder.build(apiKey, key, config, mContext);

        SplitClient client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        latch.await(20, TimeUnit.SECONDS);

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("price", 24584535);
        properties.put("id", 158576837);
        client.track("user", "long-number-event", 1, properties);
        mLatchs.get(0).await(20, TimeUnit.SECONDS);
        Thread.sleep(500);
        Event event = findEvent("long-number-event", 1.0);

        assertNotNull(event);
        assertEquals(24584535, event.properties.get("price"));
        assertEquals(158576837, event.properties.get("id"));
    }

    private String emptyChanges() {
        return "{\"splits\":[], \"since\": 9567456937869, \"till\": 9567456937869 }";
    }

    private Event findEvent(String type, Double value) {
        int i = 0;
        while (i < 3) {
            List<Event> events = mEventsHits.get(i);
            if (events != null) {
                Optional<Event> oe = events.stream()
                        .filter(event -> event.eventTypeId.equals(type) && event.value == value).findFirst();
                if (oe.isPresent()) {
                    return oe.get();
                }
            }
            i++;
        }
        return null;
    }
}
