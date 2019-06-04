import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventTaskMethodNotImplementedException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class FactoryTest {

    Context mContext;
    MockWebServer webServer;
    int curSplitReqId = 1;

    @Before
    public void setup() {
        setupServer();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @After
    public void tearDown() throws IOException {
        webServer.shutdown();
    }

    @Test
    public void testDataFolderCreation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        String apiKey = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
        String dataFolderName = "2a1099049fd8653247c5ea42bOIajMRhH0R0FcBwJZM4ca7zj6HAq1ZDS";
        File cacheDir = mContext.getCacheDir();
        File dataFolder = new File(cacheDir, dataFolderName);
        if(dataFolder.exists()) {
            boolean isDataFolderDelete = dataFolder.delete();
            log("Data folder exists and deleted: " + isDataFolderDelete);
        }

        SplitClient client;

        final String url = webServer.url("/").url().toString();

        Key key = new Key("CUSTOMER_ID",null);
        SplitClientConfig config = SplitClientConfig.builder()
                .endpoint(url, url)
                .ready(30000)
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(30)
                .enableDebug()
                .trafficType("account")
                .eventsPerPush(10)
                .build();


        SplitFactory splitFactory = SplitFactoryBuilder.build(apiKey, key, config, mContext);


        client = splitFactory.client();
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                //Assert.assertTrue(true);
                latch.countDown();
            }

            @Override
            public void onPostExecutionView(SplitClient client) {

            }
        });

        client.on(SplitEvent.SDK_READY_TIMED_OUT, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                //Assert.assertTrue(false);
                latch.countDown();
            }

            @Override
            public void onPostExecutionView(SplitClient client) {

            }
        });

        latch.await(60, TimeUnit.SECONDS);



        Assert.assertTrue(dataFolder.exists());

    }

    private void setupServer() {
        webServer = new MockWebServer();

        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch (RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("/api/mySegments")) {
                    return new MockResponse().setResponseCode(200).setBody("[\\\"segment1\\\", \\\"segment2\\\"]");
                } else if (request.getPath().contains("/api/splitChanges")) {
                    int r = curSplitReqId;
                    curSplitReqId++;
                    return new MockResponse().setResponseCode(200)
                            .setBody(splitsPerRequest(r));
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        webServer.setDispatcher(dispatcher);
    }

    private String splitsPerRequest(int reqId) {
        switch (reqId) {
            case 1:
                log("req sp 1");
                return "{\\\"splits\\\":[], \\\"since\\\":-1, \\\"till\\\":1000000000001}";
            case 2:
                log("req sp 2");
                return "{\\\"splits\\\":[], \\\"since\\\":1000000000001, \\\"till\\\":1000000000002}";
        }
        return "";
    }

    private void log(String m) {
        System.out.println("FACTORY_TEST: " + m);
    }
}
