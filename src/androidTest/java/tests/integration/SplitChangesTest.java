package tests.integration;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.DatabaseHelper;
import helper.FileHelper;
import helper.ImpressionListenerHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class SplitChangesTest {

    Context mContext;
    MockWebServer mWebServer;
    int mCurSplitReqId = 0;
    ArrayList<String> mJsonChanges = null;
    ArrayList<CountDownLatch> mLatchs;
    CountDownLatch mImpLatch;
    ArrayList<TestImpressions> mImpHits;
    private static final int CHANGE_INTERVAL = 100000;

    @Before
    public void setup() {

        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mCurSplitReqId = 0;
        mImpLatch = new CountDownLatch(1);
        mLatchs = new ArrayList<>();
        mImpHits = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            mLatchs.add(new CountDownLatch(1));
        }
        if (mJsonChanges == null) {
            loadSplitChanges();
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
                    return new MockResponse()
                            .setResponseCode(200)
                            .setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, " +
                                    "{ \"id\":\"id1\", \"name\":\"segment2\"}]}");

                } else if (request.getPath().contains("/splitChanges")) {
                    int currReq = mCurSplitReqId;
                    mCurSplitReqId++;
                    if (currReq < mLatchs.size()) {
                        if (currReq > 0) {
                            mLatchs.get(currReq - 1).countDown();
                        }
                        String changes = mJsonChanges.get(currReq);
                        return new MockResponse().setResponseCode(200).setBody(changes);
                    } else if (currReq == mLatchs.size()) {
                        mLatchs.get(currReq - 1).countDown();
                    }
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"splits\":[], \"since\": 9567456937869, \"till\": 9567456937869 }");


                } else if (request.getPath().contains("/testImpressions/bulk")) {
                    List<TestImpressions> data = IntegrationHelper.buildImpressionsFromJson(request.getBody().readUtf8());
                    mImpHits.addAll(data);
                    mImpLatch.countDown();
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
        ArrayList<String> treatments = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";

        ImpressionListenerHelper impListener = new ImpressionListenerHelper();

        SplitRoomDatabase splitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        splitRoomDatabase.clearAllTables();

        SplitClient client;

        final String url = mWebServer.url("/").url().toString();

        Key key = new Key("CUSTOMER_ID", null);
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .serviceEndpoints(ServiceEndpoints.builder().apiEndpoint(url).eventsEndpoint(url).build())
                .ready(5000)
                .featuresRefreshRate(5)
                .segmentsRefreshRate(5)
                .impressionsRefreshRate(25)
                .impressionsQueueSize(1000)
                .impressionsChunkSize(9999999)
                .streamingEnabled(false)
                .enableDebug()
                .trafficType("client")
                .impressionListener(impListener)
                .build();


        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                apiKey, key,
                config, mContext, null, splitRoomDatabase);

        client = splitFactory.client();

        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTimeOutTask);

        boolean await = latch.await(5, TimeUnit.SECONDS);

        if (!await) {
            Assert.fail("SDK not ready");
        }

        for (int i = 0; i < 4; i++) {
            boolean awaitTreatment = mLatchs.get(i).await(8, TimeUnit.SECONDS);
            if (!awaitTreatment) {
                Assert.fail("Treatment not received");
            }
            treatments.add(client.getTreatment("test_feature"));
        }
        boolean impAwait = mImpLatch.await(10, TimeUnit.SECONDS);
        if (!impAwait) {
            Assert.fail("Impressions not received");
        }
        client.destroy();
        Thread.sleep(1000);

        ArrayList<Impression> impLis = new ArrayList<>();
        impLis.add(impListener.getImpression(impListener.buildKey(
                "CUSTOMER_ID", "test_feature", "on_0")));
        impLis.add(impListener.getImpression(impListener.buildKey(
                "CUSTOMER_ID", "test_feature", "off_1")));
        impLis.add(impListener.getImpression(impListener.buildKey(
                "CUSTOMER_ID", "test_feature", "on_2")));

        for (int i = 0; i < 4; i++) {
            Assert.assertEquals(IntegrationHelper.isEven(i) ? "on_" + i : "off_" + i, treatments.get(i));
        }
        Assert.assertEquals(3, impLis.size());
        Assert.assertEquals(1567456937865L, impLis.get(0).changeNumber().longValue());
        Assert.assertEquals((1567456937865L + CHANGE_INTERVAL), impLis.get(1).changeNumber().longValue());
        Assert.assertEquals((1567456937865L + CHANGE_INTERVAL * 2), impLis.get(2).changeNumber().longValue());
        List<KeyImpression> impressions = allImpressions();
        Assert.assertEquals(4, impressions.size());
        KeyImpression imp0 = findImpression("on_0");
        KeyImpression imp3 = findImpression("off_3");
        Assert.assertEquals("on_0", imp0.treatment);
        Assert.assertEquals(1567456937865L, imp0.changeNumber.longValue());

        Assert.assertEquals("off_3", imp3.treatment);
        Assert.assertEquals(1567456937865L + CHANGE_INTERVAL * 3, imp3.changeNumber.longValue());
    }

    private void loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        mJsonChanges = new ArrayList<>();
        String jsonChange = fileHelper.loadFileContent(mContext, "splitchanges_int_test.json");
        long prevChangeNumber = 0;
        for (int i = 0; i < 4; i++) {
            SplitChange change = Json.fromJson(jsonChange, SplitChange.class);
            if (prevChangeNumber != 0) {
                change.since = prevChangeNumber;
                change.till = prevChangeNumber + CHANGE_INTERVAL;
            }
            prevChangeNumber = change.till;
            boolean even = IntegrationHelper.isEven(i);
            Split split = change.splits.get(0);
            split.changeNumber = prevChangeNumber;
            Partition p1 = split.conditions.get(0).partitions.get(0);
            Partition p2 = split.conditions.get(0).partitions.get(1);
            p1.treatment = "on_" + i;
            p1.size = (even ? 100 : 0);
            p2.treatment = "off_" + i;
            p2.size = (even ? 0 : 100);
            mJsonChanges.add(Json.toJson(change));
        }
    }

    private void log(String m) {
        System.out.println("FACTORY_TEST: " + m);
    }

    private List<KeyImpression> allImpressions() {
        List<KeyImpression> impressions = new ArrayList<>();
        int hitCount = mImpHits.size();
        for (TestImpressions timp : mImpHits) {
            for (KeyImpression imp : timp.keyImpressions) {
                impressions.add(imp);
            }
        }
        return impressions;
    }

    private KeyImpression findImpression(String treatment) {
        List<KeyImpression> impressions = allImpressions();
        if (impressions != null) {
            Optional<KeyImpression> oe = impressions.stream()
                    .filter(impression -> impression.treatment.equals(treatment)).findFirst();
            if (oe.isPresent()) {
                return oe.get();
            }
        }
        return null;
    }
}
