package tests.integration.telemetry;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.storage.TelemetryStorage;
import io.split.android.client.utils.Json;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class TelemetryIntegrationTest {

    private Context mContext;
    private SplitRoomDatabase testDatabase;
    private MockWebServer mWebServer;
    private SplitClient client;
    private AtomicInteger configEndpointHits;
    private AtomicInteger statsEndpointHits;

    @Before
    public void setUp() {
        setupServer();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        testDatabase = DatabaseHelper.getTestDatabase(mContext);
        testDatabase.clearAllTables();

        configEndpointHits = new AtomicInteger(0);
        statsEndpointHits = new AtomicInteger(0);
        initializeClient(false);
    }

    @After
    public void tearDown() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (mWebServer != null) {
            mWebServer.shutdown();
        }
        client.destroy();
        clearStorage();
    }

    @Test
    public void telemetryInitTest() {

        TelemetryStorage telemetryStorage = StorageFactory.getTelemetryStorage(true);
//        assertEquals(1, telemetryStorage.getNonReadyUsage());
        assertEquals(1, telemetryStorage.getActiveFactories());
        assertEquals(0, telemetryStorage.getRedundantFactories());
        assertTrue(telemetryStorage.getTimeUntilReadyFromCache() > 0);
        assertTrue(telemetryStorage.getTimeUntilReady() > 0);
        assertTrue(telemetryStorage.getTimeUntilReady() >= telemetryStorage.getTimeUntilReadyFromCache());
    }

    @Test
    public void telemetryEvaluationLatencyTest() {

        client.getTreatment("test_split");
        client.getTreatments(Arrays.asList("test_split", "test_split_2"), null);
        client.getTreatmentWithConfig("test_split", null);
        client.getTreatmentsWithConfig(Arrays.asList("test_split", "test_split_2"), null);
        client.track("test_traffic_type", "test_split");

        TelemetryStorage telemetryStorage = StorageFactory.getTelemetryStorage(true);
        MethodLatencies methodLatencies = telemetryStorage.popLatencies();
        assertFalse(methodLatencies.getTreatment().stream().allMatch(aLong -> aLong == 0L));
        assertFalse(methodLatencies.getTreatments().stream().allMatch(aLong -> aLong == 0L));
        assertFalse(methodLatencies.getTreatmentWithConfig().stream().allMatch(aLong -> aLong == 0L));
        assertFalse(methodLatencies.getTreatmentsWithConfig().stream().allMatch(aLong -> aLong == 0L));
        assertFalse(methodLatencies.getTrack().stream().allMatch(aLong -> aLong == 0L));
    }

    @Test
    public void recordImpressionStats() {

        client.getTreatment("test_feature");

        client.getTreatment("test_feature");

        client.getTreatment("test_feature");

        TelemetryStorage telemetryStorage = StorageFactory.getTelemetryStorage(true);
        assertEquals(1, telemetryStorage.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_QUEUED));
        assertEquals(2, telemetryStorage.getImpressionsStats(ImpressionsDataType.IMPRESSIONS_DEDUPED));
    }

    @Test
    public void recordEventsStats() {
        client.track("event", "traffic_type");

        TelemetryStorage telemetryStorage = StorageFactory.getTelemetryStorage(true);
        assertEquals(1, telemetryStorage.getEventsStats(EventsDataRecordsEnum.EVENTS_QUEUED));
    }

    @Test
    public void configIsPostedAfterInitialization() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        countDownLatch.await(1, TimeUnit.SECONDS);
        assertEquals(1, configEndpointHits.get());
    }

    @Test
    public void statsAreFlushedOnDestroy() throws InterruptedException {
        client.destroy();
        Thread.sleep(5200);

        assertEquals(1, statsEndpointHits.get());
    }

    @Test
    public void statsAreSentOnSynchronizerStart() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(5200, TimeUnit.MILLISECONDS);

        assertEquals(1, statsEndpointHits.get());
    }

    @Test
    public void recordAuthRejections() throws InterruptedException {
        client.destroy();
        CountDownLatch sseLatch = new CountDownLatch(1);
        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path.contains("/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (path.contains("/splitChanges")) {
                    long changeNumber = -1;
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"splits\":[], \"since\":" + changeNumber + ", \"till\":" + (changeNumber + 1000) + "}");
                } else if (path.contains("/events/bulk")) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.contains("metrics")) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.contains("auth")) {
                    sseLatch.countDown();
                    return new MockResponse().setResponseCode(401);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };

        mWebServer.setDispatcher(dispatcher);

        initializeClient(true);
        sseLatch.await(10, TimeUnit.SECONDS);
        Thread.sleep(1000);
        TelemetryStorage telemetryStorage = StorageFactory.getTelemetryStorage(true);

        assertEquals(1, telemetryStorage.popAuthRejections());
    }

    @Test
    public void recordSessionLength() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        initializeClient(true);
        countDownLatch.await(1, TimeUnit.SECONDS);

        client.destroy();
        countDownLatch.await(500, TimeUnit.MILLISECONDS);
        long sessionLength = StorageFactory.getTelemetryStorage(true).getSessionLength();
        assertTrue(sessionLength > 0);
    }

    private void initializeClient(boolean streamingEnabled) {
        insertSplitsFromFileIntoDB();
        CountDownLatch countDownLatch = new CountDownLatch(1);

        client = getTelemetrySplitFactory(mWebServer, streamingEnabled).client();

        SplitEventTaskHelper readyFromCacheTask = new SplitEventTaskHelper(countDownLatch);
        client.on(SplitEvent.SDK_READY, readyFromCacheTask);

        try {
            countDownLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private SplitFactory getTelemetrySplitFactory(MockWebServer webServer, boolean streamingEnabled) {
        final String url = webServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .eventsEndpoint(url)
                .telemetryServiceEndpoint(url)
                .sseAuthServiceEndpoint(url)
                .apiEndpoint(url).eventsEndpoint(url).build();

        SplitClientConfig config = new TestableSplitConfigBuilder()
                .serviceEndpoints(endpoints)
                .enableDebug()
                .featuresRefreshRate(9999)
                .segmentsRefreshRate(9999)
                .impressionsRefreshRate(9999)
                .readTimeout(3000)
                .streamingEnabled(streamingEnabled)
                .shouldRecordTelemetry(true)
                .build();

        return IntegrationHelper.buildFactory(
                "dummy_api_key",
                new Key("dummy_user_key"),
                config,
                mContext,
                null,
                testDatabase);
    }

    private void insertSplitsFromFileIntoDB() {
        List<Split> splitListFromJson = getSplitListFromJson();
        List<SplitEntity> entities = splitListFromJson.stream()
                .filter(split -> split.name != null)
                .map(split -> {
                    SplitEntity result = new SplitEntity();
                    result.setName(split.name);
                    result.setBody(Json.toJson(split));

                    return result;
                }).collect(Collectors.toList());

        testDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.DATBASE_MIGRATION_STATUS, GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE));
        testDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 1));
        testDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, System.currentTimeMillis() / 1000));

        testDatabase.splitDao().insert(entities);
    }

    private List<Split> getSplitListFromJson() {
        FileHelper fileHelper = new FileHelper();
        String s = fileHelper.loadFileContent(mContext, "splitchanges_int_test.json");

        SplitChange changes = Json.fromJson(s, SplitChange.class);

        return changes.splits;
    }

    private void setupServer() {
        mWebServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                String path = request.getPath();
                if (path.contains("/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (path.contains("/splitChanges")) {
                    long changeNumber = -1;
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"splits\":[], \"since\":" + changeNumber + ", \"till\":" + (changeNumber + 1000) + "}");
                } else if (path.contains("/events/bulk")) {
                    return new MockResponse().setResponseCode(200);
                } else if (path.contains("/metrics")) {
                    if (path.contains("/config")) {
                        configEndpointHits.incrementAndGet();
                    } else if (path.contains("/usage")) {
                        statsEndpointHits.incrementAndGet();
                    }
                    return new MockResponse().setResponseCode(200);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };

        mWebServer.setDispatcher(dispatcher);
    }

    private void clearStorage() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<StorageFactory> clazz = StorageFactory.class;
        Method method = clazz.getDeclaredMethod("clearTelemetryStorage");
        method.setAccessible(true);
        method.invoke(null, new Object[]{});
    }
}
