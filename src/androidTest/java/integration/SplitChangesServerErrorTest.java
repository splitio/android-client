import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.utils.Json;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class SplitChangesServerErrorTest {

    Context mContext;
    MockWebServer mWebServer;
    int mCurSplitReqId = 0;
    ArrayList<String> mJsonChanges = null;
    ArrayList<CountDownLatch> mLatchs;
    private static final int CHANGE_INTERVAL = 100000;

    @Before
    public void setup() {

        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mCurSplitReqId = 0;
        mLatchs = new ArrayList<>();
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

        final ArrayList<MockResponse> responses = new ArrayList<>();
        responses.add(new MockResponse().setResponseCode(200).setBody(mJsonChanges.get(0)));
        responses.add(new MockResponse().setResponseCode(500));
        responses.add(new MockResponse().setResponseCode(500));
        responses.add(new MockResponse().setResponseCode(200).setBody(mJsonChanges.get(1)));

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
                        return responses.get(currReq);
                    } else if(currReq == mLatchs.size()) {
                        mLatchs.get(currReq - 1).countDown();
                    }
                    return new MockResponse().setResponseCode(200)
                            .setBody(emptyChanges());


                } else if (request.getPath().contains("/testImpressions/bulk")) {
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
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .endpoint(url, url)
                .ready(30000)
                .featuresRefreshRate(5)
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

        client.destroy();

        Assert.assertEquals("on_0", treatments.get(0));
        Assert.assertEquals("on_0", treatments.get(1));
        Assert.assertEquals("on_0", treatments.get(2));
        Assert.assertEquals("off_1", treatments.get(3));

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

    private String emptyChanges() {
        return "{\"splits\":[], \"since\": 9567456937865, \"till\": 9567456937869 }";
    }

}
