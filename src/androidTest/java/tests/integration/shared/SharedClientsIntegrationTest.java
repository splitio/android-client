package tests.integration.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.SplitLogLevel;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class SharedClientsIntegrationTest {

    private static Context mContext;
    private SplitRoomDatabase mRoomDb;
    private SplitFactory mSplitFactory;
    private List<String> mJsonChanges = null;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, 10));
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.DATBASE_MIGRATION_STATUS, 1));

        if (mJsonChanges == null) {
            loadSplitChanges();
        }

        ServerMock mWebServer = new ServerMock(mJsonChanges);

        String serverUrl = mWebServer.getServerUrl();
        mSplitFactory = getFactory(serverUrl);

        mRoomDb.clearAllTables();
    }

    private SplitFactory getFactory(String serverUrl) {
        return IntegrationHelper.buildFactory(IntegrationHelper.dummyApiKey(),
                new Key("key1"),
                SplitClientConfig.builder()
                        .serviceEndpoints(ServiceEndpoints.builder()
                                .apiEndpoint(serverUrl).eventsEndpoint(serverUrl).build())
                        .ready(30000)
                        .logLevel(SplitLogLevel.DEBUG)
                        .featuresRefreshRate(99999)
                        .segmentsRefreshRate(99999)
                        .impressionsRefreshRate(99999)
                        .trafficType("account")
                        .streamingEnabled(true)
                        .build(),
                mContext,
                null,
                mRoomDb);
    }

    @After
    public void tearDown() throws InterruptedException {
        mSplitFactory.destroy();
    }

    @Test
    public void multipleClientsAreReadyFromCache() throws InterruptedException {
        insertSplitsIntoDb();
        Thread.sleep(1000);
        verifyEventExecution(SplitEvent.SDK_READY_FROM_CACHE);
    }

    @Test
    public void multipleClientsAreReady() throws InterruptedException {
        verifyEventExecution(SplitEvent.SDK_READY);
    }

    @Test
    public void equalMatchingKeyAndDifferentBucketingKey() throws InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch readyLatch2 = new CountDownLatch(1);

        AtomicInteger readyCount = new AtomicInteger(0);
        AtomicInteger readyCount2 = new AtomicInteger(0);

        mSplitFactory.client().on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyCount.addAndGet(1);
                readyLatch.countDown();
            }
        });
        mSplitFactory.client(new Key("key1", "bucketing")).on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyCount2.addAndGet(1);
                readyLatch2.countDown();
            }
        });
        insertSplitsIntoDb();
        readyLatch.await(10, TimeUnit.SECONDS);
        readyLatch2.await(10, TimeUnit.SECONDS);

        assertEquals(1, readyCount.get());
        assertEquals(1, readyCount2.get());
    }

    @Test
    public void bucketTest() throws InterruptedException {
        String splitName = "bucket_test";
        String userKey = "key1";
        Map<String, String> bucketKeys = new HashMap<>();

        bucketKeys.put("2643632B-D2D7-4DDF-89EA-A2563CFE317F", "V1");
        bucketKeys.put("62FAA071-E87B-4FA7-A059-CD10C8BD78C6", "V10");
        bucketKeys.put("596DBDCF-0FF3-4F07-A5F4-A2386EAD540B", "V20");
        bucketKeys.put("A4232DB6-B609-49C5-84A3-55BB80F70122", "V30");
        bucketKeys.put("E4457B93-7D9C-4E1A-B363-492FAC589077", "V40");
        bucketKeys.put("206AEA7F-0392-4159-8A64-1DAE8B20BA6D", "V50");
        bucketKeys.put("393899EB-AD1D-4943-8136-2481DE7A0875", "V60");
        bucketKeys.put("7B7AD9AC-21C7-46C0-B49B-A19BBE726409", "V70");
        bucketKeys.put("9975F10D-044A-48C8-8443-2816B92852DC", "V80");
        bucketKeys.put("DC8B43D2-5D06-48D3-B1FD-FEDF1A6DC2F1", "V90");
        bucketKeys.put("0E7C9914-7268-452A-B855-DF06542C1FE7", "V100");

        insertSplitsIntoDb();

        Map<String, AtomicInteger> readyTimes = new HashMap<>();
        Map<String, CountDownLatch> latches = new HashMap<>();
        Map<String, SplitClient> clients = new HashMap<>();

        for (Map.Entry<String, String> entry : bucketKeys.entrySet()) {
            latches.put(entry.getKey(), new CountDownLatch(1));
            readyTimes.put(entry.getKey(), new AtomicInteger(0));
            clients.put(entry.getKey(), mSplitFactory.client(new Key(userKey, entry.getKey())));
            clients.get(entry.getKey()).on(SplitEvent.SDK_READY, new SplitEventTask() {
                @Override
                public void onPostExecution(SplitClient client) {
                    readyTimes.get(entry.getKey()).addAndGet(1);
                    latches.get(entry.getKey()).countDown();
                }
            });
        }

        Map<String, Boolean> awaitResults = new HashMap<>();
        for (Map.Entry<String, CountDownLatch> entry : latches.entrySet()) {
            awaitResults.put(entry.getKey(), entry.getValue().await(20, TimeUnit.SECONDS));
        }

        Map<String, String> results = new HashMap<>();
        for (Map.Entry<String, SplitClient> entry : clients.entrySet()) {
            results.put(entry.getKey(), entry.getValue().getTreatment(splitName));
        }

        for (Map.Entry<String, Boolean> entry : awaitResults.entrySet()) {
            assertTrue(entry.getValue());
        }

        for (Map.Entry<String, AtomicInteger> entry : readyTimes.entrySet()) {
            assertEquals(1, entry.getValue().get());
        }

        for (Map.Entry<String, String> entry : bucketKeys.entrySet()) {
            assertEquals(entry.getValue(), results.get(entry.getKey()));
        }
    }

    private void verifyEventExecution(SplitEvent event) throws InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch readyLatch2 = new CountDownLatch(1);

        SplitClient client = mSplitFactory.client();

        client.on(event, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyLatch.countDown();
            }
        });

        SplitClient client2 = mSplitFactory.client(new Key("key2"));
        client2.on(event, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyLatch2.countDown();
            }
        });

        boolean ready1Await = readyLatch.await(25, TimeUnit.SECONDS);
        boolean ready2Await = readyLatch2.await(25, TimeUnit.SECONDS);

        assertTrue(ready1Await);
        assertTrue(ready2Await);
    }

    private void loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        mJsonChanges = new ArrayList<>();
        mJsonChanges.add(fileHelper.loadFileContent(mContext, "bucket_split_test.json"));
    }

    private void insertSplitsIntoDb() throws InterruptedException {
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
    }

    private static class ServerMock {

        private final MockWebServer mWebServer = new MockWebServer();
        private final List<String> mJsonChanges;
        CountDownLatch mLatchTrack = null;

        ServerMock(List<String> jsonChanges) {
            setupServer();
            mJsonChanges = jsonChanges;
        }

        private void setupServer() {

            final Dispatcher dispatcher = new Dispatcher() {

                @Override
                public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                    if (request.getPath().contains("/mySegments/key1")) {
                        return new MockResponse()
                                .setResponseCode(200)
                                .setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                    } else if (request.getPath().contains("/mySegments/key2")) {
                        return new MockResponse()
                                .setResponseCode(200)
                                .setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment2\"}]}");
                    } else if (request.getPath().contains("/splitChanges")) {
                        return new MockResponse().setResponseCode(200)
                                .setBody(splitsPerRequest());

                    } else if (request.getPath().contains("/events/bulk")) {
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

        public String getServerUrl() {
            return mWebServer.url("/").toString();
        }

        private String splitsPerRequest() {
            return mJsonChanges.get(0);
        }
    }
}
