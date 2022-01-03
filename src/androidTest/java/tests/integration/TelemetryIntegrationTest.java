package tests.integration;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
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

    @Before
    public void setUp() {
        setupServer();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        testDatabase = DatabaseHelper.getTestDatabase(mContext);
        testDatabase.clearAllTables();
    }

    @Test
    public void telemetryInitTest() throws InterruptedException {
        insertSplitsFromFileIntoDB();
        CountDownLatch countDownLatch = new CountDownLatch(1);

        SplitClient client = getTelemetrySplitFactory().client();

        // Perform usages before SDK is ready
        client.getTreatment("test");
        client.getTreatment("test");

        SplitEventTaskHelper readyFromCacheTask = new SplitEventTaskHelper(countDownLatch);
        client.on(SplitEvent.SDK_READY, readyFromCacheTask);

        countDownLatch.await(30, TimeUnit.SECONDS);

        TelemetryStorage telemetryStorage = StorageFactory.getTelemetryStorage(true);
        assertEquals(2, telemetryStorage.getNonReadyUsage());
        assertEquals(1, telemetryStorage.getActiveFactories());
        assertEquals(0, telemetryStorage.getRedundantFactories());
        assertTrue(telemetryStorage.getTimeUntilReadyFromCache() > 0);
        assertTrue(telemetryStorage.getTimeUntilReady() > 0);
        assertTrue(telemetryStorage.getTimeUntilReady() >= telemetryStorage.getTimeUntilReadyFromCache());
    }

    @After
    public void tearDown() throws IOException {
        if (mWebServer != null) {
            mWebServer.shutdown();
        }
    }

    private SplitFactory getTelemetrySplitFactory() {
        final String url = mWebServer.url("/").url().toString();
        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url).eventsEndpoint(url).build();

        SplitClientConfig config = new TestableSplitConfigBuilder()
                .serviceEndpoints(endpoints)
                .enableDebug()
                .featuresRefreshRate(9999)
                .segmentsRefreshRate(9999)
                .impressionsRefreshRate(9999)
                .readTimeout(3000)
                .streamingEnabled(false)
                .shouldRecordTelemetry(true)
                .build();

        return IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
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
                if (request.getPath().contains("/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (request.getPath().contains("/splitChanges")) {

                    long changeNumber = -1;

                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"splits\":[], \"since\":" + changeNumber + ", \"till\":" + (changeNumber + 1000) + "}");
                } else if (request.getPath().contains("/events/bulk")) {
                    return new MockResponse().setResponseCode(200);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };

        mWebServer.setDispatcher(dispatcher);
    }
}
