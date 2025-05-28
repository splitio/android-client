package tests.integration.streaming;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.reflect.TypeToken;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import fake.HttpStreamResponseMock;
import helper.DatabaseHelper;
import helper.FileHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.service.impressions.ImpressionsCount;
import io.split.android.client.service.impressions.ImpressionsCountPerFeature;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;

public class ImpressionsCountTest {
    Context mContext;
    BlockingQueue<String> mStreamingData;
    CountDownLatch mImpCountLatch;
    CountDownLatch mImpressionsLatch;
    SplitChange mSplitChange;
    final static long CHANGE_NUMBER = 99999L;

    static final Type TEST_IMPRESSIONS_TYPE = new TypeToken<ArrayList<TestImpressions>>(){}.getType();

    Map<String, ImpressionsCountPerFeature> mCounts;
    List<KeyImpression> mImpressions;

    int mImpressionsLatchLimit = 0;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStreamingData = new LinkedBlockingDeque<>();
        mImpCountLatch = new CountDownLatch(1);
        mImpressionsLatch = new CountDownLatch(1);
        loadSplitChanges();
        mCounts = new HashMap<>();
        mImpressions = new ArrayList<>();
    }

    @Test
    public void testOptimizedMode() throws IOException, InterruptedException {
        mImpressionsLatchLimit = 3;
        CountDownLatch readyLatch = new CountDownLatch(1);
        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());
        SplitRoomDatabase db = DatabaseHelper.getTestDatabase(mContext);
        db.clearAllTables();
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(3)
                .impressionsRefreshRate(3)
                .impressionsCountersRefreshRate(3)
                .impressionsChunkSize(5)
                .streamingEnabled(true)
                .legacyStorageMigrationEnabled(false)
                .impressionsMode(ImpressionsMode.OPTIMIZED)
                .enableDebug()
                .trafficType("account").build();

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock, db);

        SplitClient client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);

        client.on(SplitEvent.SDK_READY, readyTask);

        readyLatch.await(5, TimeUnit.SECONDS);

        for(int i=0; i<10; i++) {
            client.getTreatment("SPLIT_1");
            for(int j=0; j<2; j++) {
                client.getTreatment("SPLIT_2");
                for(int l=0; l<2; l++) {
                    client.getTreatment("SPLIT_3");
                }
            }
        }
        Thread.sleep(500);
        client.flush();

        mImpressionsLatch.await(10, TimeUnit.SECONDS);
        mImpCountLatch.await(10, TimeUnit.SECONDS);

        ImpressionsCountPerFeature c1 = mCounts.get("SPLIT_1");
        ImpressionsCountPerFeature c2 = mCounts.get("SPLIT_2");
        ImpressionsCountPerFeature c3 = mCounts.get("SPLIT_3");

        Assert.assertTrue(client.isReady());
        Assert.assertEquals(3, mImpressions.size());
        Assert.assertEquals(3, mCounts.size());
        Assert.assertEquals(9, c1.count);
        Assert.assertEquals(19, c2.count);
        Assert.assertEquals(39, c3.count);

        splitFactory.destroy();
    }

    @Test
    public void testDebugMode() throws IOException, InterruptedException {
        mImpressionsLatchLimit = 70;
        CountDownLatch readyLatch = new CountDownLatch(1);
        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        SplitClientConfig config = new TestableSplitConfigBuilder()
                .ready(30000)
                .featuresRefreshRate(3)
                .segmentsRefreshRate(3)
                .impressionsRefreshRate(3)
                .impressionsCountersRefreshRate(3)
                .impressionsChunkSize(5)
                .streamingEnabled(true)
                .impressionsMode(ImpressionsMode.DEBUG)
                .enableDebug()
                .trafficType("account").build();

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(), IntegrationHelper.dummyUserKey(),
                config, mContext, httpClientMock);

        SplitClient client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(readyLatch);

        client.on(SplitEvent.SDK_READY, readyTask);

        readyLatch.await(5, TimeUnit.SECONDS);

        for(int i=0; i<10; i++) {
            client.getTreatment("SPLIT_1");
            for(int j=0; j<2; j++) {
                client.getTreatment("SPLIT_2");
                for(int l=0; l<2; l++) {
                    client.getTreatment("SPLIT_3");
                }
            }
        }
        client.flush();

        mImpressionsLatch.await(10, TimeUnit.SECONDS);

        Assert.assertEquals(70, mImpressions.size());
        Assert.assertEquals(0, mCounts.size());

        splitFactory.destroy();
    }

    private HttpResponseMock createResponse(int status, String data) {
        return new HttpResponseMock(status, data);
    }

    private HttpStreamResponseMock createStreamResponse(int status, BlockingQueue<String> streamingResponseData) throws IOException {
        return new HttpStreamResponseMock(status, streamingResponseData);
    }

    private HttpResponseMockDispatcher createBasicResponseDispatcher() {
        return new HttpResponseMockDispatcher() {
            boolean first = true;
            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    return createResponse(200, IntegrationHelper.dummyAllSegments());
                } else if (uri.getPath().contains("/splitChanges")) {
                    if (first) {
                        first = false;
                        return createResponse(200, getSplitChanges());
                    }
                    return createResponse(200, IntegrationHelper.emptySplitChanges(CHANGE_NUMBER, CHANGE_NUMBER));
                } else if (uri.getPath().contains("/auth")) {
                    return createResponse(200, IntegrationHelper.streamingEnabledToken());
                } else if (uri.getPath().contains("/testImpressions/bulk")) {
                    addImpression(body);
                    return new HttpResponseMock(200);
                } else if (uri.getPath().contains("/testImpressions/count")) {
                    addCount(body);
                    return new HttpResponseMock(200);
                } else {
                    return new HttpResponseMock(200);
                }
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    return createStreamResponse(200, mStreamingData);
                } catch (Exception e) {
                }
                return null;
            }
        };
    }

    private void loadSplitChanges() {
        mSplitChange = IntegrationHelper.getChangeFromJsonString(
                loadMockedData("splitchanges_int_test.json"));

        SplitChange c1 = IntegrationHelper.getChangeFromJsonString(
                loadMockedData("splitchanges_int_test.json"));

        SplitChange c2 = IntegrationHelper.getChangeFromJsonString(
                loadMockedData("splitchanges_int_test.json"));

        mSplitChange.splits.get(0).name = "SPLIT_1";
        Split split = c1.splits.get(0);
        split.name = "SPLIT_2";
        mSplitChange.splits.add(split);
        split = c2.splits.get(0);
        split.name = "SPLIT_3";
        mSplitChange.splits.add(split);
    }

    private String getSplitChanges() {
        mSplitChange.splits.get(0).changeNumber = CHANGE_NUMBER;
        mSplitChange.till = CHANGE_NUMBER;
        return Json.toJson(TargetingRulesChange.create(mSplitChange));
    }

    private String loadMockedData(String fileName) {
        FileHelper fileHelper = new FileHelper();
        return fileHelper.loadFileContent(mContext, fileName);
    }

    private void addImpression(String body) {
        System.out.println("ADD IMPRESSION");
        List<TestImpressions> tests = Json.fromJson(body, TEST_IMPRESSIONS_TYPE);
        for (TestImpressions test : tests) {
            mImpressions.addAll(test.keyImpressions);
            if(mImpressions.size() == mImpressionsLatchLimit) {
                System.out.println("ADD IMPRESSION LATCH TRIGGERED");
                mImpressionsLatch.countDown();
            }
        }
    }

    private void addCount(String body) {
        ImpressionsCount counts = Json.fromJson(body, ImpressionsCount.class);
        for (ImpressionsCountPerFeature count : counts.perFeature) {
            ImpressionsCountPerFeature old = mCounts.get(count.feature);
            if (old == null) {
                old = new ImpressionsCountPerFeature("", count.timeframe, 0);
            }
            ImpressionsCountPerFeature theNew = new ImpressionsCountPerFeature(count.feature, count.timeframe, old.count + count.count);
            mCounts.put(count.feature, theNew);
        }
        if (mCounts.size() == 3) {
            mImpCountLatch.countDown();
        }
    }
}
