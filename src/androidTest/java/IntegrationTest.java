import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.FileHelper;
import helper.ImpressionListenerHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.SplitManager;
import io.split.android.client.api.Key;
import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Event;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.grammar.Treatments;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class IntegrationTest {

    Context mContext;
    MockWebServer mWebServer;
    int mCurSplitReqId = 1;
    List<String> mTrackEndpointHits = null;
    List<String> mJsonChanges = null;
    String mTrackRequestBody = null;
    CountDownLatch mLatchTrack = null;

    @Before
    public void setup() {
        setupServer();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mCurSplitReqId = 1;
        mTrackEndpointHits = new ArrayList<>();
        mLatchTrack = null;
        if(mJsonChanges == null) {
            loadSplitChanges();
        }
    }

    @After
    public void tearDown() throws IOException {
        mWebServer.shutdown();
    }

    private void setupServer() {
        mWebServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch (RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("/api/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (request.getPath().contains("/api/splitChanges")) {
                    int r = mCurSplitReqId;
                    mCurSplitReqId++;
                    return new MockResponse().setResponseCode(200)
                            .setBody(splitsPerRequest(r));
                } else if (request.getPath().contains("/api/events/bulk")) {
                    String trackRequestBody = request.getBody().readUtf8();
                    mTrackEndpointHits.add(trackRequestBody);
                    if(mLatchTrack != null) {
                        mLatchTrack.countDown();
                    }
                    return new MockResponse().setResponseCode(200);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    @Test
    public void testAll() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        mLatchTrack = new CountDownLatch(10);
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
        String dataFolderName = "2a1099049fd8653247c5ea42bOIajMRhH0R0FcBwJZM4ca7zj6HAq1ZDS";
        File cacheDir = mContext.getCacheDir();
        ImpressionListenerHelper impListener = new ImpressionListenerHelper();

        File dataFolder = new File(cacheDir, dataFolderName);
        if(dataFolder.exists()) {
            boolean isDataFolderDelete = dataFolder.delete();
            log("Data folder exists and deleted: " + isDataFolderDelete);
        }

        SplitClient client;
        SplitManager manager;

        final String url = mWebServer.url("/").url().toString();

        Key key = new Key("CUSTOMER_ID",null);
        SplitClientConfig config = SplitClientConfig.builder()
                .endpoint(url, url)
                .ready(30000)
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(30)
                .enableDebug()
                .trafficType("account")
                .eventsPerPush(10)
                .eventsQueueSize(100)
                .impressionListener(impListener)
                .build();


        SplitFactory splitFactory = SplitFactoryBuilder.build(apiKey, key, config, mContext);

        client = splitFactory.client();
        manager = splitFactory.manager();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        latch.await(40, TimeUnit.SECONDS);
        String t1 = client.getTreatment("FACUNDO_TEST");
        String t2 = client.getTreatment("NO_EXISTING_FEATURE");

        Map<String, String> ts1 = client.getTreatments(Arrays.asList("testing222", "NO_EXISTING_FEATURE1", "NO_EXISTING_FEATURE2"), null);

        SplitView s1 = manager.split("FACUNDO_TEST");
        SplitView s2 = manager.split("NO_EXISTING_FEATURE");
        List<SplitView> splits = manager.splits();

        Impression i1 = impListener.getImpression(impListener.buildKey("CUSTOMER_ID", "FACUNDO_TEST", "off"));
        Impression i2 = impListener.getImpression(impListener.buildKey("CUSTOMER_ID", "NO_EXISTING_FEATURE", Treatments.CONTROL));

        for(int i=0; i<101; i++) {
            client.track("account", i);
        }

        mLatchTrack.await(30, TimeUnit.SECONDS);

        List<Event> lastTrackHitEvents = buildEventsFromJson(mTrackEndpointHits.get(mTrackEndpointHits.size() -1));
        Event event99 = findEventWithValue(lastTrackHitEvents, 99.0);
        Event event100 = findEventWithValue(lastTrackHitEvents, 100.0);

        Assert.assertTrue(dataFolder.exists());
        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertFalse(readyTimeOutTask.isOnPostExecutionCalled);
        Assert.assertEquals("off", t1);
        Assert.assertEquals(Treatments.CONTROL, t2);
        Assert.assertEquals("off", ts1.get("testing222"));
        Assert.assertEquals(Treatments.CONTROL, ts1.get("NO_EXISTING_FEATURE1"));
        Assert.assertEquals(Treatments.CONTROL, ts1.get("NO_EXISTING_FEATURE2"));
        Assert.assertEquals(29, splits.size());
        Assert.assertNotNull(s1);
        Assert.assertNull(s2);
        Assert.assertNotNull(i1);
        Assert.assertNull(i2);
        Assert.assertEquals("not in split", i1.appliedRule());
        Assert.assertEquals(10, mTrackEndpointHits.size());
        Assert.assertNotNull(event99);
        Assert.assertNull(event100);
    }


    @Test
    public void testSdkTimeout() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";

        SplitClient client;

        Key key = new Key("CUSTOMER_ID",null);
        SplitClientConfig config = SplitClientConfig.builder()
                .ready(30000)
                .featuresRefreshRate(99999)
                .segmentsRefreshRate(99999)
                .impressionsRefreshRate(99999)
                .trafficType("account")
                .build();


        SplitFactory splitFactory = SplitFactoryBuilder.build(apiKey, key, config, mContext);

        client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        latch.await(40, TimeUnit.SECONDS);

        Assert.assertFalse(readyTask.isOnPostExecutionCalled);
        Assert.assertTrue(readyTimeOutTask.isOnPostExecutionCalled);
    }

    private String splitsPerRequest(int reqId) {
        int req = mJsonChanges.size() - 1;
        if(reqId < req) {
            req = reqId;
        }
        return mJsonChanges.get(req);
    }

    private void loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        mJsonChanges = new ArrayList<>();
        for(int i=0; i<1; i++) {
            String changes = fileHelper.loadFileContent(mContext,"split_changes_" + (i + 1) + ".json");
            mJsonChanges.add(changes);
        }
    }

    private List<Event> buildEventsFromJson(String attributesJson) {

        GsonBuilder gsonBuilder = new GsonBuilder();

        Type mapType = new TypeToken<List<Event>>(){}.getType();
        Gson gson = gsonBuilder.create();
        List<Event> events;
        try {
            events = gson.fromJson(attributesJson, mapType);
        } catch (Exception e) {
            events =  Collections.emptyList();
        }

        return events;
    }

    private Event findEventWithValue(List<Event> events, double value) {
        for(Event event : events) {
            if(value == event.value) {
                return event;
            }
        }
        return null;
    }

    private void log(String m) {
        System.out.println("FACTORY_TEST: " + m);
    }

}
