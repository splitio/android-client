package tests.integration;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.DatabaseHelper;
import helper.FileHelper;
import helper.ImpressionListenerHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.SplitManager;
import io.split.android.client.SplitResult;
import io.split.android.client.api.Key;
import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.SplitLogLevel;
import io.split.android.grammar.Treatments;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SuppressWarnings({"UnstableApiUsage", "ResultOfMethodCallIgnored"})
public class IntegrationTest {

    Context mContext;
    MockWebServer mWebServer;
    int mCurSplitReqId = 1;
    List<String> mTrackEndpointHits = null;
    List<String> mJsonChanges = null;
    CountDownLatch mLatchTrack = null;

    @Before
    public void setup() {
        setupServer();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mCurSplitReqId = 1;
        mTrackEndpointHits = new ArrayList<>();
        mLatchTrack = null;
        if (mJsonChanges == null) {
            loadSplitChanges();
        }
    }

    private void setupServer() {
        mWebServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (request.getPath().contains("/splitChanges")) {
                    int r = mCurSplitReqId;
                    mCurSplitReqId++;
                    return new MockResponse().setResponseCode(200)
                            .setBody(splitsPerRequest(r));
                } else if (request.getPath().contains("/events/bulk")) {
                    String trackRequestBody = request.getBody().readUtf8();
                    mTrackEndpointHits.add(trackRequestBody);
                    if (mLatchTrack != null) {
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
        CountDownLatch readyFromCacheLatch = new CountDownLatch(1);
        mLatchTrack = new CountDownLatch(10);
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
        ImpressionListenerHelper impListener = new ImpressionListenerHelper();

        SplitRoomDatabase splitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        splitRoomDatabase.clearAllTables();
        splitRoomDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.DATBASE_MIGRATION_STATUS, GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE));
        splitRoomDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 2));
        SplitClient client;
        SplitManager manager;

        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url).eventsEndpoint(url).build();
        Key key = new Key("CUSTOMER_ID");
        SplitClientConfig config = SplitClientConfig.builder()
                .serviceEndpoints(endpoints)
                .ready(30000)
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(30)
                .eventFlushInterval(200)
                .logLevel(SplitLogLevel.DEBUG)
                .trafficType("account")
                .eventsPerPush(10)
                .eventsQueueSize(100)
                .impressionListener(impListener)
                .build();

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                apiKey, key,
                config, mContext, null, splitRoomDatabase);

        client = splitFactory.client();
        manager = splitFactory.manager();
        SplitEventTaskHelper readyFromCacheTask = new SplitEventTaskHelper(readyFromCacheLatch);
        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_FROM_CACHE, readyFromCacheTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        readyFromCacheLatch.await(40, TimeUnit.SECONDS);
        latch.await(40, TimeUnit.SECONDS);

        String t1 = client.getTreatment("FACUNDO_TEST");
        String t2 = client.getTreatment("NO_EXISTING_FEATURE");
        SplitResult treatmentConfigEmojis = client.getTreatmentWithConfig("Welcome_Page_UI", null);

        Map<String, String> ts1 = client.getTreatments(Arrays.asList("testing222", "NO_EXISTING_FEATURE1", "NO_EXISTING_FEATURE2"), null);

        SplitView s1 = manager.split("FACUNDO_TEST");
        SplitView s2 = manager.split("NO_EXISTING_FEATURE");
        List<SplitView> splits = manager.splits();

        Impression i1 = impListener.getImpression(impListener.buildKey("CUSTOMER_ID", "FACUNDO_TEST", "off"));
        Impression i2 = impListener.getImpression(impListener.buildKey("CUSTOMER_ID", "NO_EXISTING_FEATURE", Treatments.CONTROL));

        for (int i = 0; i < 101; i++) {
            Map<String, Object> props = new HashMap<>();
            props.clear();
            switch (i) {
                case 98:
                    props.put("stringKey", "pepe");
                    props.put("numberKey", 1.3);
                    props.put("booleanKey", true);
                    props.put("nullKey", null);
                    client.track("account", i, props);
                    break;
                case 99:
                    props.put("stringKey", "pepe");
                    props.put("numberKey", 1.3);
                    props.put("booleanKey", true);
                    props.put("nullKey", null);
                    props.put("listKey", Arrays.asList("a", "b", "c"));
                    client.track("account", i, props);
                    break;
                default:
                    client.track("account", i);
            }

        }

        mLatchTrack.await(40, TimeUnit.SECONDS);

        List<Event> lastTrackHitEvents = buildEventsFromJson(mTrackEndpointHits.get(mTrackEndpointHits.size() - 1));
        Event event98 = findEventWithValue(lastTrackHitEvents, 98.0);
        Event event99 = findEventWithValue(lastTrackHitEvents, 99.0);
        Event event100 = findEventWithValue(lastTrackHitEvents, 100.0);
        Map<String, Object> props98 = event98.properties;
        Map<String, Object> props99 = event99.properties;

        Assert.assertTrue(client.isReady());
        Assert.assertTrue(splitFactory.isReady());
        Assert.assertTrue(readyFromCacheTask.isOnPostExecutionCalled);
        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertFalse(readyTimeOutTask.isOnPostExecutionCalled);

        Assert.assertEquals("off", t1);
        Assert.assertEquals(Treatments.CONTROL, t2);
        Assert.assertEquals("off", treatmentConfigEmojis.treatment());
        Assert.assertEquals("{\"the_emojis\":\"\uD83D\uDE01 -- áéíóúöÖüÜÏëç\"}", treatmentConfigEmojis.config());
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
        Assert.assertNotNull(event98);
        Assert.assertNotNull(event99);
        Assert.assertNull(event100);

        Assert.assertEquals("pepe", props98.get("stringKey"));
        Assert.assertEquals(1.3, props98.get("numberKey"));
        Assert.assertEquals(true, props98.get("booleanKey"));
        Assert.assertEquals(null, props98.get("nullKey"));

        Assert.assertEquals("pepe", props99.get("stringKey"));
        Assert.assertEquals(1.3, props99.get("numberKey"));
        Assert.assertEquals(true, props99.get("booleanKey"));
        Assert.assertEquals(null, props99.get("nullKey"));
        Assert.assertEquals(null, props99.get("listKey"));

        splitFactory.destroy();
    }

    @Test
    public void testNoReadyFromCache() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
        String dataFolderName = "2a1099049fd8653247c5ea42bOIajMRhH0R0FcBwJZM4ca7zj6HAq1ZDS";
        ImpressionListenerHelper impListener = new ImpressionListenerHelper();

        SplitRoomDatabase splitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        splitRoomDatabase.clearAllTables();
        splitRoomDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.DATBASE_MIGRATION_STATUS, GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE));
        splitRoomDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, -1));
        SplitClient client;
        SplitManager manager;

        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url).eventsEndpoint(url).build();
        Key key = new Key("CUSTOMER_ID", null);
        SplitClientConfig config = SplitClientConfig.builder()
                .serviceEndpoints(endpoints)
                .ready(30000)
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(30)
                .logLevel(SplitLogLevel.DEBUG)
                .trafficType("account")
                .eventsPerPush(10)
                .eventsQueueSize(100)
                .impressionListener(impListener)
                .build();

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                apiKey, key,
                config, mContext, null, splitRoomDatabase);

        client = splitFactory.client();
        manager = splitFactory.manager();
        SplitEventTaskHelper readyFromCacheTask = new SplitEventTaskHelper();
        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_FROM_CACHE, readyFromCacheTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        latch.await(40, TimeUnit.SECONDS);

        Assert.assertTrue(client.isReady());
        Assert.assertTrue(splitFactory.isReady());
        Assert.assertFalse(readyFromCacheTask.isOnPostExecutionCalled);
        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertFalse(readyTimeOutTask.isOnPostExecutionCalled);

        splitFactory.destroy();
    }

    @Test
    public void testGetTreatmentFromCache() throws Exception {

        SplitRoomDatabase mRoomDb;
        Context mContext;
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 10));
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.DATBASE_MIGRATION_STATUS, 1));

        SplitChange change = Json.fromJson(mJsonChanges.get(0), SplitChange.class);
        List<SplitEntity> entities = new ArrayList<>();
        for (Split split : change.splits) {
            String splitName = split.name;
            SplitEntity entity = new SplitEntity();
            entity.setName(splitName);
            entity.setBody(Json.toJson(split));
            entities.add(entity);
        }
        mRoomDb.splitDao().insert(entities);

        CountDownLatch latch = new CountDownLatch(1);

        SplitClient client;

        Key key = new Key("CUSTOMER_ID", null);
        SplitClientConfig config = SplitClientConfig.builder()
                .ready(30000)
                .featuresRefreshRate(99999)
                .segmentsRefreshRate(99999)
                .impressionsRefreshRate(99999)
                .trafficType("account")
                .streamingEnabled(false)
                .build();


        SplitFactory splitFactory = IntegrationHelper.buildFactory(apiKey, key, config, mContext, null, mRoomDb);

        client = splitFactory.client();
        SplitManager manager = splitFactory.manager();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper();

        client.on(SplitEvent.SDK_READY_FROM_CACHE, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        latch.await(40, TimeUnit.SECONDS);

        String t1 = client.getTreatment("FACUNDO_TEST");
        String t2 = client.getTreatment("NO_EXISTING_FEATURE");
        SplitResult treatmentConfigEmojis = client.getTreatmentWithConfig("Welcome_Page_UI", null);

        Map<String, String> ts1 = client.getTreatments(Arrays.asList("testing222", "NO_EXISTING_FEATURE1", "NO_EXISTING_FEATURE2"), null);

        SplitView s1 = manager.split("FACUNDO_TEST");
        SplitView s2 = manager.split("NO_EXISTING_FEATURE");

        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertFalse(readyTimeOutTask.isOnPostExecutionCalled);

        Assert.assertEquals("off", t1);
        Assert.assertEquals(Treatments.CONTROL, t2);
        Assert.assertEquals("off", treatmentConfigEmojis.treatment());
        Assert.assertEquals("{\"the_emojis\":\"\uD83D\uDE01 -- áéíóúöÖüÜÏëç\"}", treatmentConfigEmojis.config());
        Assert.assertEquals("off", ts1.get("testing222"));
        Assert.assertEquals(Treatments.CONTROL, ts1.get("NO_EXISTING_FEATURE1"));
        Assert.assertEquals(Treatments.CONTROL, ts1.get("NO_EXISTING_FEATURE2"));

        splitFactory.destroy();
    }

    @Test
    public void testSdkTimeout() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";

        SplitClient client;

        Key key = new Key("CUSTOMER_ID", null);
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

        splitFactory.destroy();
    }

    @After
    public void tearDown() throws IOException {
        mWebServer.shutdown();
    }

    private String splitsPerRequest(int reqId) {
        int req = mJsonChanges.size() - 1;
        if (reqId < req) {
            req = reqId;
        }
        return mJsonChanges.get(req);
    }

    private void loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        mJsonChanges = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            String changes = fileHelper.loadFileContent(mContext, "split_changes_" + (i + 1) + ".json");
            mJsonChanges.add(changes);
        }
    }

    private List<Event> buildEventsFromJson(String attributesJson) {

        GsonBuilder gsonBuilder = new GsonBuilder();

        Type mapType = new TypeToken<List<Event>>() {
        }.getType();
        Gson gson = gsonBuilder.create();
        List<Event> events;
        try {
            events = gson.fromJson(attributesJson, mapType);
        } catch (Exception e) {
            events = Collections.emptyList();
        }

        return events;
    }

    private Event findEventWithValue(List<Event> events, double value) {
        for (Event event : events) {
            if (value == event.value) {
                return event;
            }
        }
        return null;
    }

    private void log(String m) {
        System.out.println("FACTORY_TEST: " + m);
    }

}
