package tests.integration;


import static android.os.SystemClock.sleep;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.HttpStreamResponseMock;
import fake.LifecycleManagerStub;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.NetworkHelperStub;
import helper.SplitEventTaskHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SingleSyncTest {

    private Context mContext;
    String mUserKey = "key";
    int mUniqueKeysHitCount = 0;
    int mImpressionsCountHitCount = 0;
    int mImpressionsHitCount = 0;
    int mEventsHitCount = 0;
    int mMySegmentsHitCount = 0;
    int mSplitsHitCount = 0;
    int mSseAuthHitCount = 0;

    CountDownLatch mImpLatch;
    CountDownLatch mEveLatch;
    CountDownLatch mImpCountLatch;
    CountDownLatch mKeyLatch;

    LifecycleManagerStub mLifecycleManager;

    @Before
    public void setup() throws IOException {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mLifecycleManager = new LifecycleManagerStub();
        mUniqueKeysHitCount = 3;
        mImpressionsCountHitCount = 0;
        mImpressionsHitCount = 3;
        mEventsHitCount = 3;
        mMySegmentsHitCount = 0;
        mSplitsHitCount = 0;
        mSseAuthHitCount = 0;
    }

    private SplitFactory buildFactory(ImpressionsMode impressionsMode) {
        SplitRoomDatabase splitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        splitRoomDatabase.clearAllTables();
        HttpResponseMockDispatcher dispatcher = buildDispatcher();

        SplitClientConfig config = new TestableSplitConfigBuilder().ready(30000)
                .streamingEnabled(true)
                .syncEnabled(false)
                .enableDebug()
                .trafficType("account")
                .impressionsMode(impressionsMode)
                .impressionsRefreshRate(1)
                .impressionsCountersRefreshRate(1)
                .eventFlushInterval(1)
                .build();

        try {

            SplitFactory factory = IntegrationHelper.buildFactory(
                    IntegrationHelper.dummyApiKey(),
                    IntegrationHelper.dummyUserKey(),
                    config,
                    mContext,
                    new HttpClientMock(dispatcher),
                    splitRoomDatabase, null, new NetworkHelperStub(), null,
                    mLifecycleManager);

            return factory;
        } catch (IOException e) {
        }
        return null;
    }

    @Test
    public void singleSyncEnabledImpressionsOptimized() throws Exception {
        SplitFactory factory = buildFactory(ImpressionsMode.OPTIMIZED);
        SplitClient client = factory.client();

        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(readyLatch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        readyLatch.await(5, TimeUnit.SECONDS);

        generateData(factory, client);

        mImpLatch = new CountDownLatch(1);
        mEveLatch = new CountDownLatch(1);
        mImpCountLatch = new CountDownLatch(1);

        simulateBgFg(); // Make the SDK to store impressions count

        mImpLatch.await(5, TimeUnit.SECONDS);
        mEveLatch.await(5, TimeUnit.SECONDS);
        mImpCountLatch.await(5, TimeUnit.SECONDS);

        Assert.assertEquals(1, mSplitsHitCount);
        Assert.assertEquals(4, mMySegmentsHitCount); // One for key
        Assert.assertEquals(0, mSseAuthHitCount);
        Assert.assertTrue(mEventsHitCount > 3);
        Assert.assertTrue(mImpressionsHitCount > 3);
        Assert.assertTrue(mImpressionsCountHitCount > 0);

        client.destroy();
    }

    @Test
    public void singleSyncEnabledImpressionsDebug() throws Exception {
        SplitFactory factory = buildFactory(ImpressionsMode.DEBUG);
        SplitClient client = factory.client();

        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(readyLatch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        readyLatch.await(5, TimeUnit.SECONDS);

        generateData(factory, client);

        mImpLatch = new CountDownLatch(1);
        mEveLatch = new CountDownLatch(1);

        simulateBgFg(); // Make the SDK to store impressions count

        mImpLatch.await(5, TimeUnit.SECONDS);
        mEveLatch.await(5, TimeUnit.SECONDS);

        sleep(500); // Some time to make sure no impressions count are sent

        Assert.assertEquals(1, mSplitsHitCount);
        Assert.assertEquals(4, mMySegmentsHitCount); // One for key
        Assert.assertEquals(0, mSseAuthHitCount);
        Assert.assertTrue(mEventsHitCount > 3);
        Assert.assertTrue(mImpressionsHitCount > 3);
        Assert.assertEquals(0, mImpressionsCountHitCount);

        client.destroy();
    }

    @Test
    public void singleSyncEnabledImpressionsNone() throws Exception {
        SplitFactory factory = buildFactory(ImpressionsMode.NONE);
        SplitClient client = factory.client();

        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(readyLatch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        readyLatch.await(5, TimeUnit.SECONDS);

        generateData(factory, client);

        mKeyLatch = new CountDownLatch(1);
        mEveLatch = new CountDownLatch(1);
        mImpCountLatch = new CountDownLatch(1);

        simulateBgFg(); // Make the SDK to store impressions count

        mKeyLatch.await(5, TimeUnit.SECONDS);
        mEveLatch.await(5, TimeUnit.SECONDS);
        mImpCountLatch.await(5, TimeUnit.SECONDS);

        Assert.assertEquals(1, mSplitsHitCount);
        Assert.assertEquals(4, mMySegmentsHitCount); // One for key
        Assert.assertEquals(0, mSseAuthHitCount);
        Assert.assertTrue(mEventsHitCount > 3);
        Assert.assertTrue(mUniqueKeysHitCount > 3);
        Assert.assertTrue(mImpressionsCountHitCount > 0);

        client.destroy();
    }

    void simulateBgFg() {
        mLifecycleManager.simulateOnPause();
        mLifecycleManager.simulateOnResume();
    }

    private void generateData(SplitFactory factory, SplitClient client) throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            CountDownLatch readyLatch = new CountDownLatch(1);
            SplitClient cli = factory.client("key" + i);
            cli.on(SplitEvent.SDK_READY, new SplitEventTask() {
                @Override
                public void onPostExecution(SplitClient client) {
                    client.getTreatment("TEST");
                    readyLatch.countDown();
                }

                @Override
                public void onPostExecutionView(SplitClient client) {
                    client.getTreatment("TEST");
                }
            });
            readyLatch.await(5, TimeUnit.SECONDS);
            client.track("eve" + i);
        }
    }

    @NonNull
    private HttpResponseMockDispatcher buildDispatcher() {
        return new HttpResponseMockDispatcher() {

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    return createStreamResponse(200, new LinkedBlockingDeque<>());
                } catch (Exception e) {
                    Logger.e("** SSE Connect error: " + e.getLocalizedMessage());
                }
                return null;
            }

            @Override

            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                System.out.println(uri.getPath());
                if (uri.getPath().contains("/mySegments")) {
                    mMySegmentsHitCount++;
                    return new HttpResponseMock(200, IntegrationHelper.emptyMySegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    mSplitsHitCount++;
                    return getSplitsMockResponse("", "");
                } else if (uri.getPath().contains("/testImpressions/bulk")) {
                    mImpressionsHitCount++;
                    if(mImpLatch != null) {
                        mImpLatch.countDown();
                    }
                    return new HttpResponseMock(200);

                } else if (uri.getPath().contains("/testImpressions/count")) {
                    mImpressionsCountHitCount++;
                    if(mImpressionsCountHitCount > 0 && mImpCountLatch != null) {
                        mImpCountLatch.countDown();
                    }
                    return new HttpResponseMock(200);

                } else if (uri.getPath().contains("/events/bulk")) {
                    mEventsHitCount++;
                    if(mEveLatch != null) {
                        mEveLatch.countDown();
                    }
                    return new HttpResponseMock(200);

                } else if (uri.getPath().contains("/keys/cs")) {
                    mUniqueKeysHitCount++;
                    if(mKeyLatch != null) {
                        mKeyLatch.countDown();
                    }
                    return new HttpResponseMock(200);
                } else if (uri.getPath().contains("/auth")) {
                    mSseAuthHitCount++;
                    return new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken());
                } else {
                    return new HttpResponseMock(404);
                }
            }
        };
    }

    @NonNull
    private HttpResponseMock getSplitsMockResponse(final String since, final String till) {
        return new HttpResponseMock(200, loadSplitChanges());
    }

    private HttpStreamResponseMock createStreamResponse(int status, BlockingQueue<String> streamingResponseData) throws IOException {
        return null;
    }

    private String loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        String change = fileHelper.loadFileContent(mContext,"split_changes_1.json");
        SplitChange parsedChange = Json.fromJson(change, SplitChange.class);
        parsedChange.since = parsedChange.till;
        return Json.toJson(parsedChange);
    }
}
