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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import helper.FileHelper;
import helper.ImpressionListenerHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.Matcher;
import io.split.android.client.dtos.MatcherCombiner;
import io.split.android.client.dtos.MatcherGroup;
import io.split.android.client.dtos.MatcherType;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.dtos.UserDefinedSegmentMatcherData;
import io.split.android.client.dtos.Condition;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class MySegmentUpdatedTest {

    Context mContext;
    MockWebServer mWebServer;
    int mCurReqId = 0;
    ArrayList<String> mJsonChanges = null;
    ArrayList<CountDownLatch> mLatchs;
    CountDownLatch mImpLatch;
    ArrayList<TestImpressions> mImpHits;
    boolean isFirstChangesReq;

    @Before
    public void setup() {
        isFirstChangesReq = true;
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mCurReqId = 0;
        mImpLatch = new CountDownLatch(2);
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

                    String data;
                    int index = mCurReqId;
                    switch (index) {
                        case 1:
                            data = "{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}]}";
                            break;
                        case 2:
                            data = "{\"mySegments\":[{ \"id\":\"id2\", \"name\":\"segment2\"}]}";
                            break;
                        default:
                            data = "{\"mySegments\":[]}";
                    }

                    if(index > 0 && index <= mLatchs.size()) {
                        mLatchs.get(index - 1).countDown();
                        Thread.sleep(1000);
                    }
                    mCurReqId++;
                    return new MockResponse().setResponseCode(200).setBody(data);

                } else if (request.getPath().contains("/splitChanges")) {
                    if(isFirstChangesReq) {
                        isFirstChangesReq = false;
                        String change = mJsonChanges.get(0);
                        return new MockResponse().setResponseCode(200)
                                .setBody(change);
                    }
                    return new MockResponse().setResponseCode(200)
                            .setBody(emptyChanges());


                } else if (request.getPath().contains("/testImpressions/bulk")) {
                    mImpHits.addAll(IntegrationHelper.buildImpressionsFromJson(request.getBody().readUtf8()));
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
        String dataFolderName = "2a1099049fd8653247c5ea42bOIajMRhH0R0FcBwJZM4ca7zj6HAq1ZDS";

        ImpressionListenerHelper impListener = new ImpressionListenerHelper();

        SplitRoomDatabase splitRoomDatabase = SplitRoomDatabase.getDatabase(mContext, dataFolderName);
        splitRoomDatabase.clearAllTables();

        SplitClient client;

        final String url = mWebServer.url("/").url().toString();

        Key key = new Key("CUSTOMER_ID", null);
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .endpoint(url, url)
                .ready(30000)
                .featuresRefreshRate(5)
                .segmentsRefreshRate(5)
                .impressionsRefreshRate(21)
                .impressionsChunkSize(999999)
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

        for (int i = 0; i < 4; i++) {
            mLatchs.get(i).await(20, TimeUnit.SECONDS);
            treatments.add(client.getTreatment("test_feature"));
        }
        mImpLatch.await(30, TimeUnit.SECONDS);
        client.destroy();

        Assert.assertEquals("no", treatments.get(0));
        Assert.assertEquals("on_s1", treatments.get(1));
        Assert.assertEquals("on_s2", treatments.get(2));
        Assert.assertEquals("no", treatments.get(3));

        List<KeyImpression> impressions = allImpressions();
        Assert.assertEquals(4, impressions.size());
        KeyImpression imp0 = findImpression("no");
        KeyImpression imp1 = findImpression("on_s1");
        KeyImpression imp2 = findImpression("on_s2");

        Assert.assertNotNull(imp0);
        Assert.assertNotNull(imp1);
        Assert.assertNotNull(imp2);
    }

    private void loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        mJsonChanges = new ArrayList<>();
        String jsonChange = fileHelper.loadFileContent(mContext, "splitchanges_int_test.json");

        SplitChange change = Json.fromJson(jsonChange, SplitChange.class);

        Split split = change.splits.get(0);
        split.changeNumber = change.since + 1;

        Condition inSegmentOneCondition = inSegmentCondition("segment1");
        Partition s1p1 = inSegmentOneCondition.partitions.get(0);
        Partition s1p2 = inSegmentOneCondition.partitions.get(1);
        s1p1.size = 100;
        s1p1.treatment = "on_s1";
        s1p2.size = 0;
        s1p2.treatment = "off_s1";

        Condition inSegmentTwoCondition = inSegmentCondition("segment2");
        Partition s2p1 = inSegmentTwoCondition.partitions.get(0);
        Partition s2p2 = inSegmentTwoCondition.partitions.get(1);
        s2p1.size = 100;
        s2p1.treatment = "on_s2";
        s2p2.size = 0;
        s2p2.treatment = "off_s2";

        split.conditions.add(0, inSegmentOneCondition);
        split.conditions.add(0, inSegmentTwoCondition);

        mJsonChanges.add(Json.toJson(change));
    }

    private Condition inSegmentCondition(String name) {
        Condition condition = new Condition();
        MatcherGroup matcherGroup = new MatcherGroup();
        Matcher matcher = new Matcher();
        UserDefinedSegmentMatcherData matcherData = new UserDefinedSegmentMatcherData();
        condition.partitions = new ArrayList();
        condition.partitions.add(new Partition());
        condition.partitions.add(new Partition());
        condition.label = "in segment " + name;
        matcherData.segmentName = name;
        matcherGroup.combiner = MatcherCombiner.AND;
        condition.conditionType = ConditionType.WHITELIST;
        condition.matcherGroup = matcherGroup;
        matcher.matcherType = MatcherType.IN_SEGMENT;
        matcher.userDefinedSegmentMatcherData = matcherData;
        matcherGroup.matchers = new ArrayList<>();
        matcherGroup.matchers.add(matcher);
        return condition;
    }

    private void log(String m) {
        System.out.println("FACTORY_TEST: " + m);
    }

    private String emptyChanges() {
        return "{\"splits\":[], \"since\": 9567456937865, \"till\": 9567456937869 }";
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
        KeyImpression imp = null;

        if (impressions != null) {
            Optional<KeyImpression> oe = impressions.stream()
                    .filter(impression -> impression.treatment.equals(treatment)).findFirst();
            if (oe.isPresent()) {
                imp = oe.get();
            }
        }
        return imp;
    }

}
