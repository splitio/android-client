package tests.integration;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import io.split.sharedtest.fake.HttpStreamResponseMock;
import helper.FileHelper;
import helper.ImpressionListenerHelper;
import helper.IntegrationHelper;
import helper.SplitEventTaskHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.SplitFactoryImpl;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.network.HttpMethod;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

public class SplitChangesServerErrorTest {

    Context mContext;
    int mCurSplitReqId = 0;
    ArrayList<String> mJsonChanges = null;
    ArrayList<CountDownLatch> mLatchs;
    private static final int CHANGE_INTERVAL = 100000;
    ArrayList<HttpResponseMock> mResponses;

    @Before
    public void setup() {

        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mCurSplitReqId = 0;
        mLatchs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mLatchs.add(new CountDownLatch(1));
        }
        if (mJsonChanges == null) {
            loadSplitChanges();
        }
        setupServer();
    }

    @After
    public void tearDown() throws IOException {
    }

    private void setupServer() {
        mResponses = new ArrayList<>();
        mResponses.add(new HttpResponseMock(200, mJsonChanges.get(0)));
        mResponses.add(new HttpResponseMock(200, mJsonChanges.get(1)));
        mResponses.add(new HttpResponseMock(500));
        mResponses.add(new HttpResponseMock(500));
        mResponses.add(new HttpResponseMock(200, mJsonChanges.get(2)));
    }

    private HttpResponseMockDispatcher createBasicResponseDispatcher() {
        return new HttpResponseMockDispatcher() {

            @Override
            public HttpResponseMock getResponse(URI uri, HttpMethod method, String body) {
                if (uri.getPath().contains("/mySegments")) {
                    return new HttpResponseMock(200, "{\"mySegments\":[{ \"id\":\"id1\", \"name\":\"segment1\"}, " +
                            "{ \"id\":\"id1\", \"name\":\"segment2\"}]}");

                } else if (uri.getPath().contains("/splitChanges")) {
                    int currReq = mCurSplitReqId;
                    mCurSplitReqId++;
                    if (currReq < mLatchs.size()) {
                        if (currReq > 0) {
                            mLatchs.get(currReq - 1).countDown();
                        }
                        System.out.println(" RESP: " + mResponses.get(currReq));
                        return mResponses.get(currReq);
                    } else if (currReq == mLatchs.size()) {
                        mLatchs.get(currReq - 1).countDown();
                    }
                    new HttpResponseMock(200, emptyChanges());

                } else if (uri.getPath().contains("/testImpressions/bulk")) {
                    return new HttpResponseMock(200);
                }
                return new HttpResponseMock(404);
            }

            @Override
            public HttpStreamResponseMock getStreamResponse(URI uri) {
                try {
                    return null;
                } catch (Exception e) {
                }
                return null;
            }
        };
    }

    @Test
    public void test() throws Exception {
        ArrayList<String> treatments = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
        SplitRoomDatabase splitRoomDatabase = DatabaseHelper.getTestDatabase(mContext);
        splitRoomDatabase.clearAllTables();

        ImpressionListenerHelper impListener = new ImpressionListenerHelper();

        SplitClient client;

        HttpClientMock httpClientMock = new HttpClientMock(createBasicResponseDispatcher());

        Key key = new Key("CUSTOMER_ID", null);
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .ready(30000)
                .featuresRefreshRate(2)
                .enableDebug()
                .trafficType("client")
                .impressionListener(impListener)
                .streamingEnabled(false)
                .build();

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                apiKey, key, config, mContext, httpClientMock, splitRoomDatabase);

        client = splitFactory.client();

        SplitEventTaskHelper readyTask = new SplitEventTaskHelper(latch);
        SplitEventTaskHelper readyTimeOutTask = new SplitEventTaskHelper(latch);

        client.on(SplitEvent.SDK_READY, readyTask);
        client.on(SplitEvent.SDK_READY_TIMED_OUT, readyTimeOutTask);

        latch.await(20, TimeUnit.SECONDS);

        for (int i = 0; i < 5; i++) {
            System.out.println("PRev EVAL");
            try {
                mLatchs.get(i).await(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
            justWait(300);
            String res = client.getTreatment("test_feature");
            treatments.add(res);
        }

        client.destroy();

        Assert.assertEquals("on", treatments.get(0));
        Assert.assertEquals("on", treatments.get(1));
        Assert.assertEquals("on", treatments.get(2));
        Assert.assertEquals("off", treatments.get(3));
    }

    private void loadSplitChanges() {
        FileHelper fileHelper = new FileHelper();
        mJsonChanges = new ArrayList<>();
        String jsonChange = fileHelper.loadFileContent(mContext, "splitchanges_int_test.json");
        long prevChangeNumber = 0;
        for (int i = 0; i < 3; i++) {
            SplitChange change = Json.fromJson(jsonChange, SplitChange.class);
            if (prevChangeNumber != 0) {
                change.since = prevChangeNumber + CHANGE_INTERVAL;
                change.till = prevChangeNumber + CHANGE_INTERVAL;
            }
            prevChangeNumber = change.till;
            boolean even = IntegrationHelper.isEven(i);
            Split split = change.splits.get(0);
            split.changeNumber = prevChangeNumber;
            Partition p1 = split.conditions.get(0).partitions.get(0);
            Partition p2 = split.conditions.get(0).partitions.get(1);
            p1.treatment = "on";
            p1.size = (i < 2 ? 100 : 0);
            p2.treatment = "off";
            p2.size = (i < 2 ? 0 : 100);
            mJsonChanges.add(Json.toJson(change));
        }
    }

    private void log(String m) {
        System.out.println("FACTORY_TEST: " + m);
    }

    private String emptyChanges() {
        return "{\"splits\":[], \"since\": 9567456937869, \"till\": 9567456937869 }";
    }

    private void justWait(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
