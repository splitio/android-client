package integration;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.ImpressionListenerHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Event;
import io.split.android.client.events.SplitEvent;
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
                    synchronized(this) {
                        int index = mCurReqId;
                        if(index > 0 && index < 4) {
                            code = 500;
                        } else {
                            List<Event> data = IntegrationHelper.buildEventsFromJson(request.getBody().readUtf8());
                            mEventsHits.add(data);
                            code = 200;
                        }

                        if(index < 6) {
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
        ArrayList<String> treatments = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
        String dataFolderName = "2a1099049fd8653247c5ea42bOIajMRhH0R0FcBwJZM4ca7zj6HAq1ZDS";

        ImpressionListenerHelper impListener = new ImpressionListenerHelper();

        File cacheDir = mContext.getCacheDir();

        File dataFolder = new File(cacheDir, dataFolderName);
        if (dataFolder.exists()) {
            File[] files = dataFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            boolean isDataFolderDelete = dataFolder.delete();
            log("Data folder exists and deleted: " + isDataFolderDelete);
        }

        SplitClient client;

        final String url = mWebServer.url("/").url().toString();

        Key key = new Key("CUSTOMER_ID", null);
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .endpoint(url, url)
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

        Map<String,Object> prop = new HashMap<>();

        for (int i = 0; i < 5; i++) {
            prop.put("value", i);
            client.track("custom", "event1", i, prop);
        }
        mLatchs.get(0).await(20, TimeUnit.SECONDS);
        trackCount.add(mEventsHits.size());

        for (int i = 0; i < 5; i++) {
            prop.put("value", i);
            client.track("custom", "event2", i, prop);
        }
        mLatchs.get(1).await(20, TimeUnit.SECONDS);
        trackCount.add(mEventsHits.size());

        for (int i = 0; i < 5; i++) {
            prop.put("value", i);
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

        Event e1 = findEvent("event1", 0.0);
        Event e2 = findEvent("event2", 2.0);
        Event e3 = findEvent("event3", 3.0);

        Assert.assertEquals("custom", e1.trafficTypeName);
        Assert.assertEquals(0.0, e1.value, 0.0);
        Assert.assertEquals("event1", e1.eventTypeId);
        Assert.assertEquals(0, ((Double) e1.properties.get("value")).intValue());

        Assert.assertEquals("custom", e2.trafficTypeName);
        Assert.assertEquals(2.0, e2.value, 0.0);
        Assert.assertEquals("event2", e2.eventTypeId);
        Assert.assertEquals(2, ((Double) e2.properties.get("value")).intValue());

        Assert.assertEquals("custom", e3.trafficTypeName);
        Assert.assertEquals(3.0, e3.value, 0.0);
        Assert.assertEquals("event3", e3.eventTypeId);
        Assert.assertEquals(3, ((Double) e3.properties.get("value")).intValue());
    }

    private void log(String m) {
        System.out.println("FACTORY_TEST: " + m);
    }

    private String emptyChanges() {
        return "{\"splits\":[], \"since\": 9567456937865, \"till\": 9567456937869 }";
    }

    private Event findEvent(String type, Double value) {
        int i = 0;
        while (i < 3) {
            List<Event> events = mEventsHits.get(i);
            if(events != null) {
                Optional<Event> oe = events.stream()
                        .filter(event -> event.eventTypeId.equals(type) && event.value == value).findFirst();
                if(oe.isPresent()) {
                    return oe.get();
                }
            }
            i++;
        }
        return null;
    }
}
