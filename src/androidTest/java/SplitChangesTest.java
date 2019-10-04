import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
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
import io.split.android.client.SplitResult;
import io.split.android.client.api.Key;
import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.impressions.Impression;
import io.split.android.client.utils.Json;
import io.split.android.grammar.Treatments;
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
        for(int i = 0; i<4; i++) {
            mLatchs.add(new CountDownLatch(1));
        }
        if(mJsonChanges == null) {
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
            public MockResponse dispatch (RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("/mySegments")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setBody("{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, " +
                                    "{ \"id\":\"id1\", \"name\":\"segment2\"}]}");

                } else if (request.getPath().contains("/splitChanges")) {
                    int currReq = mCurSplitReqId;
                    mCurSplitReqId++;
                    if(currReq < mLatchs.size()) {
                        if(currReq > 0) {
                            mLatchs.get(currReq - 1).countDown();
                        }
                        String changes = mJsonChanges.get(currReq);
                        return new MockResponse().setResponseCode(200).setBody(changes);
                    } else if(currReq == mLatchs.size()) {
                        mLatchs.get(currReq - 1).countDown();
                    }
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"splits\":[], \"since\": 9567456937865, \"till\": 9567456937869 }");


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

        File cacheDir = mContext.getCacheDir();

        File dataFolder = new File(cacheDir, dataFolderName);
        if(dataFolder.exists()) {
            File[] files = dataFolder.listFiles();
            if(files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            boolean isDataFolderDelete = dataFolder.delete();
            log("Data folder exists and deleted: " + isDataFolderDelete);
        }

        SplitClient client;

        final String url = mWebServer.url("/").url().toString();

        Key key = new Key("CUSTOMER_ID",null);
        SplitClientConfig config = SplitClientConfig.builder()
                .endpoint(url, url)
                .ready(30000)
                .featuresRefreshRate(5)
                .segmentsRefreshRate(5)
                .impressionsRefreshRate(30)
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

        for(int i=0; i<4; i++) {
            mLatchs.get(i).await(20, TimeUnit.SECONDS);
            treatments.add(client.getTreatment("test_feature"));
        }
        mImpLatch.await(30, TimeUnit.SECONDS);
        client.destroy();

        ArrayList<Impression> impLis = new ArrayList<>();
        impLis.add(impListener.getImpression(impListener.buildKey(
                "CUSTOMER_ID", "test_feature", "on_0")));
        impLis.add(impListener.getImpression(impListener.buildKey(
                "CUSTOMER_ID", "test_feature", "off_1")));
        impLis.add(impListener.getImpression(impListener.buildKey(
                "CUSTOMER_ID", "test_feature", "on_2")));

        for(int i=0; i<4; i++) {
            Assert.assertEquals(isEven(i) ? "on_" + i : "off_" + i, treatments.get(i));
        }
        Assert.assertEquals(3, impLis.size());
        Assert.assertEquals(1567456937865L, impLis.get(0).changeNumber().longValue());
        Assert.assertEquals((1567456937865L + CHANGE_INTERVAL), impLis.get(1).changeNumber().longValue());
        Assert.assertEquals((1567456937865L + CHANGE_INTERVAL * 2), impLis.get(2).changeNumber().longValue());
        Assert.assertEquals(1, mImpHits.size());
        Assert.assertEquals(4, mImpHits.get(0).keyImpressions.size());
        KeyImpression imp0 = mImpHits.get(0).keyImpressions.get(0);
        KeyImpression imp3 = mImpHits.get(0).keyImpressions.get(3);
        Assert.assertEquals("on_0", imp0.treatment);
        Assert.assertEquals(1567456937865L, imp0.changeNumber.longValue());

        Assert.assertEquals("off_3", imp3.treatment);
        Assert.assertEquals(1567456937865L  + CHANGE_INTERVAL * 3, imp3.changeNumber.longValue());
    }

    private void loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        mJsonChanges = new ArrayList<>();
        String jsonChange = fileHelper.loadFileContent(mContext,"splitchanges_int_test.json");
        long prevChangeNumber = 0;
        for(int i=0; i<4; i++) {
            SplitChange change = Json.fromJson(jsonChange, SplitChange.class);
            if(prevChangeNumber != 0) {
                change.since = prevChangeNumber;
                change.till = prevChangeNumber + CHANGE_INTERVAL;
            }
            prevChangeNumber = change.till;
            boolean even = isEven(i);
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

    private boolean isEven(int i) {
        return ((i + 2) % 2) == 0;
    }

}
