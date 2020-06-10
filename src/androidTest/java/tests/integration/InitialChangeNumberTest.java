package tests.integration;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class InitialChangeNumberTest {

    Context mContext;
    MockWebServer mWebServer;
    long mFirstChangeNumberReceived;
    boolean mIsFirstChangeNumber = true;
    final long INITIAL_CHANGE_NUMBER = 1568396481;

    @Before
    public void setup() {
        setupServer();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mFirstChangeNumberReceived = -1;
        mIsFirstChangeNumber = true;
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
                if (request.getPath().contains("/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, { \"id\":\"id1\", \"name\":\"segment2\"}]}");
                } else if (request.getPath().contains("/splitChanges")) {

                    long changeNumber = -1;
                    if(mIsFirstChangeNumber) {
                        String path = request.getPath();
                        changeNumber = Long.valueOf(path.substring(path.indexOf("=") + 1));
                        mFirstChangeNumberReceived = changeNumber;
                        mIsFirstChangeNumber = false;
                    }
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"splits\":[], \"since\":" + changeNumber + ", \"till\":" + (changeNumber + 1000) + "}");
                } else if (request.getPath().contains("/events/bulk")) {
                    String trackRequestBody = request.getBody().readUtf8();

                    return new MockResponse().setResponseCode(200);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        mWebServer.setDispatcher(dispatcher);
    }

    @Test
    public void firstRequestChangeNumber() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch readyFromCacheLatch = new CountDownLatch(1);
        Pair<String, String> apiKeyAndDb = IntegrationHelper.dummyApiKeyAndDb();
        String apiKey = apiKeyAndDb.first;
        String dataFolderName = apiKeyAndDb.second;
        SplitRoomDatabase splitRoomDatabase = SplitRoomDatabase.getDatabase(mContext, dataFolderName);
        splitRoomDatabase.clearAllTables();
        splitRoomDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.DATBASE_MIGRATION_STATUS, GeneralInfoEntity.DATBASE_MIGRATION_STATUS_DONE));
        splitRoomDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, INITIAL_CHANGE_NUMBER));
        splitRoomDatabase.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, System.currentTimeMillis() / 1000));

        SplitClient client;

        final String url = mWebServer.url("/").url().toString();

        ServiceEndpoints endpoints = ServiceEndpoints.builder()
                .apiEndpoint(url).eventsEndpoint(url).build();

        Key key = new Key("CUSTOMER_ID",null);
        SplitClientConfig config = SplitClientConfig.builder()
                .serviceEndpoints(endpoints)
                .ready(30000)
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(99999)
                .streamingEnabled(false)
                .enableDebug()
                .build();


        SplitFactory splitFactory = SplitFactoryBuilder.build(apiKey, key, config, mContext);

        client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyFromCacheTask = new SplitEventTaskHelper(readyFromCacheLatch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);
        client.on(SplitEvent.SDK_READY_FROM_CACHE, readyFromCacheTask);

        latch.await(40, TimeUnit.SECONDS);
        readyFromCacheLatch.await(40, TimeUnit.SECONDS);

        Assert.assertTrue(readyTask.isOnPostExecutionCalled);
        Assert.assertTrue(readyFromCacheTask.isOnPostExecutionCalled);
        Assert.assertEquals(INITIAL_CHANGE_NUMBER, mFirstChangeNumberReceived); // Checks that change number is the bigger number from cached splitss
    }

    private void log(String m) {
        System.out.println("FACTORY_TEST: " + m);
    }

}
